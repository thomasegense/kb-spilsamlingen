package dk.kb.spilsamlingen.api.v1.impl;

import dk.kb.spilsamlingen.api.v1.*;

import dk.kb.spilsamlingen.model.v1.ErrorDto;
import dk.kb.spilsamlingen.solr.SolrServerClient;

import java.io.File;
import java.util.List;
import java.util.Map;

import dk.kb.util.webservice.exception.ServiceException;
import dk.kb.util.webservice.exception.InternalServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.File;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dk.kb.util.webservice.ImplBase;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.model.wadl.Description;
import org.apache.cxf.jaxrs.model.wadl.DocTarget;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.multipart.*;

import io.swagger.annotations.Api;

/**
 * kb-spilsamlingen
 *
 * <p>kb-spilsamlingen by the Royal Danish Library ${config:config.openapi.injection}
 *
 */
public class KbSpilsamlingenApiServiceImpl extends ImplBase implements KbSpilsamlingenApi {
    private Logger log = LoggerFactory.getLogger(this.toString());

    @Override
    public String search(String q) throws ServiceException {

        try {
          //  String solrUrl= ServiceConfig.getSolrUrl();
            String solrUrl="http://localhost:8983/solr/spilsamlingen/";
            SolrServerClient client = new SolrServerClient(solrUrl); 
            return client.searchJsonResponse(q);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalServiceException(e.getMessage());
        }
    }

    

}
