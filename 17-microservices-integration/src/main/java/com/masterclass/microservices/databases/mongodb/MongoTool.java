package com.masterclass.microservices.databases.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MongoTool {

    private static final Logger log = LoggerFactory.getLogger(MongoTool.class);
    private static final int MAX_DOCS = 10;

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public MongoTool(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Executes a MongoDB find query on a specified collection and returns documents as JSON.
            Use this when the user needs to retrieve unstructured or semi-structured data
            (logs, product catalogs, user profiles, event records) from MongoDB.
            Input: collectionName (e.g. 'products'), filterJson (a MongoDB query filter as JSON,
            use '{}' to return all documents). Results limited to 10 documents.
            Returns: JSON array of matching documents.
            """)
    public String queryMongoDB(String collectionName, String filterJson) {
        try {
            BasicQuery query = new BasicQuery(filterJson);
            query.limit(MAX_DOCS);
            List<Document> docs = mongoTemplate.find(query, Document.class, collectionName);
            return objectMapper.writeValueAsString(docs);
        } catch (Exception e) {
            log.error("MongoDB query failed: collection={} filter={}", collectionName, filterJson, e);
            return "MongoDB query failed: " + e.getMessage();
        }
    }

    @Tool(description = """
            Lists all collection names in the MongoDB database.
            Call this first to discover which collections exist before querying.
            Returns: JSON array of collection name strings.
            """)
    public String listMongoCollections() {
        return mongoTemplate.getCollectionNames().toString();
    }

    @Tool(description = """
            Inserts a document into a MongoDB collection.
            Use this to persist agent-generated content, decisions, or derived data
            into an unstructured document store.
            Input: collectionName, documentJson (a valid JSON object string).
            Returns: the inserted document's ID.
            """)
    public String insertMongoDocument(String collectionName, String documentJson) {
        try {
            Document doc = Document.parse(documentJson);
            mongoTemplate.insert(doc, collectionName);
            log.debug("MongoDB insert: collection={} id={}", collectionName, doc.getObjectId("_id"));
            return "Inserted document with _id: " + doc.getObjectId("_id");
        } catch (Exception e) {
            log.error("MongoDB insert failed", e);
            return "Insert failed: " + e.getMessage();
        }
    }
}
