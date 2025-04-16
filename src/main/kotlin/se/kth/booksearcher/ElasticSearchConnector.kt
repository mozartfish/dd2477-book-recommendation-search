//package se.kth.booksearcher
//
//import org.apache.http.HttpHost
//import org.elasticsearch.client.RestClient
//import org.elasticsearch.client.RestHighLevelClient
//import org.elasticsearch.client.indices.GetIndexRequest
//import org.elasticsearch.client.RequestOptions
//
///**
// * Handles connections to Elasticsearch for the BookSearcher application.
// */
//class ElasticsearchConnector {
//    private val client: RestHighLevelClient
//
//    init {
//        // Create connection to the Docker container
//        client = RestHighLevelClient(
//            RestClient.builder(
//                HttpHost("localhost", 9200, "http")
//            )
//        )
//    }
//
//    fun testConnection(): Boolean {
//        return try {
//            // Check if we can connect to Elasticsearch
//            val request = GetIndexRequest("_all")
//            client.indices().exists(request, RequestOptions.DEFAULT)
//            println("Successfully connected to Elasticsearch!")
//            true
//        } catch (e: Exception) {
//            println("Failed to connect to Elasticsearch: ${e.message}")
//            e.printStackTrace()
//            false
//        }
//    }
//
//    fun close() {
//        client.close()
//    }
//}