# Lyrics Search Feature Documentation

## Overview
This document provides comprehensive information about the lyrics search feature implemented in the Spotify application using Elasticsearch. The feature allows users to search for songs by their lyrics content with advanced search capabilities.

## Architecture

### Components
1. **Elasticsearch Integration**: Full-text search engine for lyrics
2. **Database Storage**: PostgreSQL for persistent lyrics storage
3. **Search API**: RESTful endpoints for lyrics search
4. **Custom Analyzers**: Optimized text analysis for lyrics content

### Tech Stack
- **Spring Boot 3.3.2**
- **Spring Data Elasticsearch**
- **Elasticsearch 8.11.0**
- **PostgreSQL 15**
- **Kibana 8.11.0** (for monitoring and debugging)

## Features

### Search Types
1. **BASIC**: Multi-field fuzzy search across lyrics, title, artist, and album
2. **EXACT_PHRASE**: Exact phrase matching with configurable slop
3. **FUZZY**: Typo-tolerant search using Elasticsearch fuzzy queries
4. **ADVANCED**: Boosted search with field-specific scoring
5. **POPULARITY**: Search results ranked by song popularity

### Advanced Capabilities
- **Auto-completion**: Real-time suggestions as users type
- **Highlighting**: Snippet extraction with matched terms highlighted
- **Synonyms**: Support for common lyrical synonyms (love/affection, sad/melancholy)
- **Stemming**: English language stemming for better matching
- **Popularity Scoring**: Results ranked by play count, likes, and dislikes

## API Endpoints

### Search Endpoints

#### Basic Search
```http
GET /api/v1/lyrics/search?query=love&searchType=BASIC&page=0&size=20
```

#### Advanced Search
```http
POST /api/v1/lyrics/search
Content-Type: application/json

{
  "query": "broken heart",
  "searchType": "ADVANCED",
  "artistName": "Artist Name",
  "isPremium": false,
  "page": 0,
  "size": 20,
  "sortBy": "popularity",
  "sortDirection": "desc"
}
```

#### Autocomplete
```http
GET /api/v1/lyrics/autocomplete?query=love
```

### Management Endpoints

#### Add/Update Lyrics
```http
POST /api/v1/lyrics/{songId}/lyrics
Authorization: Bearer {artist_or_admin_token}
Content-Type: text/plain

[Lyrics content here]
```

#### Remove Lyrics
```http
DELETE /api/v1/lyrics/{songId}/lyrics
Authorization: Bearer {artist_or_admin_token}
```

#### Reindex All Songs
```http
POST /api/v1/lyrics/reindex
Authorization: Bearer {admin_token}
```

## Response Format

### Search Response
```json
{
  "songs": [
    {
      "songId": 1,
      "title": "Song Title",
      "artistName": "Artist Name",
      "albumName": "Album Name",
      "lyricsSnippet": "...matching lyrics snippet...",
      "relevanceScore": 2.5,
      "playCount": 1000,
      "likes": 50,
      "dislikes": 2,
      "isPremium": false,
      "publishDate": "2024-01-01T00:00:00",
      "highlightedLyrics": [
        "Line with matching words",
        "Another matching line"
      ]
    }
  ],
  "metadata": {
    "query": "search query",
    "totalHits": 25,
    "pageNumber": 0,
    "pageSize": 20,
    "totalPages": 2,
    "searchTimeMs": 45,
    "searchType": "BASIC",
    "suggestions": ["suggested queries"]
  }
}
```

## Configuration

### Application Properties
```properties
# Elasticsearch Configuration
spring.elasticsearch.uris=http://localhost:9200
spring.elasticsearch.username=${ELASTICSEARCH_USERNAME:}
spring.elasticsearch.password=${ELASTICSEARCH_PASSWORD:}
spring.elasticsearch.connection-timeout=10s
spring.elasticsearch.socket-timeout=30s

# Elasticsearch custom settings
elasticsearch.index.songs=songs-lyrics
elasticsearch.index.replicas=0
elasticsearch.index.shards=1
```

### Index Settings
The Elasticsearch index uses custom analyzers:
- **lyrics_analyzer**: Standard tokenizer with stemming and synonyms
- **autocomplete_analyzer**: Edge n-gram tokenizer for auto-completion
- **keyword_lowercase**: Exact matching with case insensitivity

## Database Schema

### SongLyrics Table
```sql
CREATE TABLE song_lyrics (
    id BIGSERIAL PRIMARY KEY,
    song_id BIGINT NOT NULL UNIQUE,
    lyrics TEXT,
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (song_id) REFERENCES song_info(id)
);
```

## Setup Instructions

### 1. Start Required Services
```bash
# Start Elasticsearch, Kibana, and PostgreSQL
docker-compose up -d

# Verify Elasticsearch is running
curl http://localhost:9200/_cluster/health
```

### 2. Build and Run Application
```bash
# Build the application
mvn clean package

# Run the application
java -jar target/Spotify-0.0.1-SNAPSHOT.jar
```

### 3. Index Sample Data
```bash
# Reindex all songs (requires admin token)
curl -X POST http://localhost:8545/api/v1/lyrics/reindex \
  -H "Authorization: Bearer {admin_token}"
```

## Usage Examples

### Search for Songs with "love" in Lyrics
```javascript
const response = await fetch('/api/v1/lyrics/search?query=love&searchType=BASIC');
const data = await response.json();
console.log(`Found ${data.metadata.totalHits} songs in ${data.metadata.searchTimeMs}ms`);
```

### Advanced Search with Filters
```javascript
const searchRequest = {
  query: "broken heart",
  searchType: "ADVANCED",
  artistName: "Taylor Swift",
  sortBy: "popularity",
  page: 0,
  size: 10
};

const response = await fetch('/api/v1/lyrics/search', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(searchRequest)
});
```

### Add Lyrics to a Song
```javascript
const lyrics = `Verse 1:
Love is a beautiful thing
When it comes to you and me...`;

const response = await fetch(`/api/v1/lyrics/${songId}/lyrics`, {
  method: 'POST',
  headers: {
    'Content-Type': 'text/plain',
    'Authorization': `Bearer ${artistToken}`
  },
  body: lyrics
});
```

## Performance Considerations

### Indexing Strategy
- **Batch Indexing**: Use `batchIndexSongs()` for large datasets
- **Incremental Updates**: Index lyrics immediately upon creation/update
- **Background Reindexing**: Schedule periodic reindexing during low-traffic hours

### Search Optimization
- **Result Caching**: Implement Redis caching for frequent queries
- **Query Limiting**: Limit search results to prevent large response payloads
- **Timeout Configuration**: Set appropriate timeouts for Elasticsearch queries

### Elasticsearch Tuning
```json
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "max_result_window": 10000,
    "refresh_interval": "30s"
  }
}
```

## Monitoring and Analytics

### Kibana Dashboards
Access Kibana at `http://localhost:5601` to:
- Monitor search performance
- Analyze search patterns
- Debug query issues
- View index statistics

### Key Metrics to Monitor
- Search response times
- Index size and growth
- Popular search queries
- Failed search requests
- Elasticsearch cluster health

## Security Considerations

### Authentication
- **User Search**: Requires authenticated user (`isAuthenticated()`)
- **Lyrics Management**: Requires ARTIST or ADMIN role
- **System Operations**: Requires ADMIN role only

### Input Validation
- Query length limits (2-500 characters)
- Sanitized filename handling
- SQL injection prevention
- XSS protection in responses

## Error Handling

### Common Error Scenarios
1. **Elasticsearch Connection Issues**: Graceful degradation with database fallback
2. **Invalid Search Queries**: Validation errors with helpful messages
3. **Large Result Sets**: Pagination and result limiting
4. **Index Corruption**: Automatic reindexing capabilities

### Error Response Format
```json
{
  "timestamp": "2024-01-01T00:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Search query must be between 2 and 500 characters",
  "path": "/api/v1/lyrics/search"
}
```

## Future Enhancements

### Planned Features
1. **Multi-language Support**: Lyrics in multiple languages
2. **AI-Powered Suggestions**: Machine learning for better search suggestions
3. **Semantic Search**: Vector-based similarity search
4. **Real-time Indexing**: WebSocket-based real-time updates
5. **Advanced Analytics**: Search behavior analytics and recommendations

### Scalability Improvements
1. **Elasticsearch Cluster**: Multi-node Elasticsearch setup
2. **Read Replicas**: Database read replicas for search queries
3. **CDN Integration**: Cache popular search results
4. **Microservice Architecture**: Separate lyrics service

## Troubleshooting

### Common Issues

#### Elasticsearch Not Starting
```bash
# Check Elasticsearch logs
docker logs spotify-elasticsearch

# Verify memory settings
docker stats spotify-elasticsearch
```

#### Search Results Not Found
```bash
# Check index status
curl http://localhost:9200/songs-lyrics/_stats

# Verify documents are indexed
curl http://localhost:9200/songs-lyrics/_count
```

#### Performance Issues
```bash
# Monitor search performance
curl http://localhost:9200/_nodes/stats/indices/search

# Check slow query log
curl http://localhost:9200/_nodes/stats/indices/search?filter_path=**.search.slowlog
```

## Support and Maintenance

### Regular Maintenance Tasks
1. **Index Optimization**: Monthly index optimization
2. **Backup Strategy**: Regular Elasticsearch snapshots
3. **Performance Monitoring**: Daily performance reviews
4. **Security Updates**: Regular dependency updates

### Contact Information
For technical support or feature requests, please contact the development team or create an issue in the project repository.
