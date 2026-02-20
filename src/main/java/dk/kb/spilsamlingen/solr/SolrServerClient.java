package dk.kb.spilsamlingen.solr;

import dk.kb.util.webservice.exception.InternalServiceException;
import dk.kb.util.webservice.exception.InvalidArgumentServiceException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Create a Solr client.
 * See the yaml-configuration to see which Solr URLs are used
 */
public class SolrServerClient {
    private static final Logger log = LoggerFactory.getLogger(SolrServerClient.class);
    private String serverUrl = null;


    // determines the amount of documents returned by the query
    private final int rows = 1000;
    protected HttpSolrClient solrServer; 
    
    public static void main(String[] args) throws Exception{
        SolrServerClient client = new SolrServerClient("http://localhost:8983/solr/transcription/");
        
     
        System.out.println("got json:");
        
        String json = client.searchJsonResponse("*:*");
        Thread.sleep(5000L);
        System.out.println("got json:"+json);
    }
    
    /**
     * Automatically populates the List of SolrServerClient when the class gets initialized
     */
 
    /**
     * Create a Solr client from a serverUrl that is used to call Solr
     *
     * @param serverUrl
     */
    public SolrServerClient(String serverUrl) {
        try {
            this.serverUrl = serverUrl;
            solrServer = new HttpSolrClient.Builder(serverUrl).build();
   //         solrServer.setParser(new NoOpResponseParser("json"));
   
        } catch (RuntimeException e) {
            log.error("Unable to connect to solr-server: {}", serverUrl, e);
        }
    }

    public String getServerUrl() {
        return serverUrl;
    }

    
    public SolrClient getSolrServer() {
        return solrServer;
    }
    
    public QueryResponse query(SolrParams solrParams) throws SolrServerException, IOException {
        return solrServer.query(solrParams);
    }

    
   public  void addDocument(HashMap<String,Object> fields) throws Exception{
        
        SolrInputDocument doc = new SolrInputDocument();

        
        for (String key :fields.keySet()) {
           Object values= fields.get(key);
           
           if (values instanceof String) {
               String valueStr=(String) values;
               if (values != null && !"".equals(valueStr.trim())) {
                    doc.setField(key,valueStr);            
               }
           }
           else if (values instanceof List) {
               List<String> valueList = (List<String>) values;
               for (String value: valueList) {
                 doc.addField(key, value);                   
               }
               
               
           }
            
        }               
        UpdateResponse updateResponse = solrServer.add(doc,0);
     System.out.println(updateResponse);
        //embeddedServer.commit();   
    }

   public String searchJsonResponse(String queryStr) throws Exception{
       SolrQuery query = new SolrQuery();
       query.set("q", queryStr);        
       query.setRows(10);
       query.set("facet", "true");
       query.set("wt", "json");
       //we want fields returned in this order. This is all fields (49)
       //but does not work. Solr determine order of fields.
       //query.set("fl","score,id,warc_date_header,warc_info_id,content_length,warc_block_digest,content_type,warc_target_uri,source_file_offset,source_file_path,title_mods,title_pb_core,access_condition,file_size_premis,file_id,parent,parent_type,parent_sub_type,parent_collection,child,format_location_pb_core,format_media_type_pb_core,format_standard_pb_core,format_colors_pb_core,format_channel_configuration_pb_core,format_identifier_pb_core,format_identifier_source_pb_core,format_name_premis,format_version_premis,format_registry_name_premis,format_registry_key_premis,format_note_premis,mets_version,mets_type,mets_creator,mods_version,date_created_mods,description_mods,pid_handle,identifier_pb_core,identifier_source_pb_core,description_pb_core,genre_pb_core,publisher_pb_core,date_available_start_pb_core,date_available_end_pb_core,premis_version,checksum_algorithm_premis,checksum_premis,_version_");
               
       NoOpResponseParser rawJsonResponseParser = new NoOpResponseParser();
       rawJsonResponseParser.setWriterType("json");

       QueryRequest req = new QueryRequest(query);
       req.setResponseParser(rawJsonResponseParser);

       NamedList<Object> resp = solrServer.request(req);
       String jsonResponse = (String) resp.get("response");
       return jsonResponse;              
       
   }
/*
   public  ArrayList<WarcRecordHistoryDto> getWarcRecordHistory(String warcTargetUri) throws Exception{
       log.info("input warcTargetUri:"+warcTargetUri);
       SolrQuery query = new SolrQuery();
       String queryStr=SolrFields.WARC_TARGET_URI+":\"" +warcTargetUri +"\"";
       log.info("Search warc history with query:"+queryStr);
       query.set("q", queryStr);        
       query.setRows(1000);
       query.set("facet", "false");
       query.set("fl",SolrFields.ID +","+SolrFields.WARC_TARGET_URI+","+SolrFields.WARC_DATE_HEADER);
       
       //we want fields returned in this order. This is all fields (49)
       //but does not work. Solr determine order of fields.
       //query.set("fl","score,id,warc_date_header,warc_info_id,content_length,warc_block_digest,content_type,warc_target_uri,source_file_offset,source_file_path,title_mods,title_pb_core,access_condition,file_size_premis,file_id,parent,parent_type,parent_sub_type,parent_collection,child,format_location_pb_core,format_media_type_pb_core,format_standard_pb_core,format_colors_pb_core,format_channel_configuration_pb_core,format_identifier_pb_core,format_identifier_source_pb_core,format_name_premis,format_version_premis,format_registry_name_premis,format_registry_key_premis,format_note_premis,mets_version,mets_type,mets_creator,mods_version,date_created_mods,description_mods,pid_handle,identifier_pb_core,identifier_source_pb_core,description_pb_core,genre_pb_core,publisher_pb_core,date_available_start_pb_core,date_available_end_pb_core,premis_version,checksum_algorithm_premis,checksum_premis,_version_");
               
       
       QueryResponse rsp = solrServer.query(query, METHOD.POST);
       SolrDocumentList results = rsp.getResults();
       
       
       ArrayList<WarcRecordHistoryDto> records=new  ArrayList<WarcRecordHistoryDto>();
       for (SolrDocument doc : results) {
           WarcRecordHistoryDto record= new WarcRecordHistoryDto();         
           record.setId((String)  doc.getFieldValue(SolrFields.ID));
           record.setWarcTargetUri((String)  doc.getFieldValue(SolrFields.WARC_TARGET_URI));
           record.setWarcDate((String)  doc.getFieldValue(SolrFields.WARC_DATE_HEADER));
           records.add(record);
       }
                            
       return records;
       
   }
   */
   
   
   
}

