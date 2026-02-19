package dk.kb.spilsamlingen.webservice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import dk.kb.spilsamlingen.api.v1.impl.KbSpilsamlingenApiServiceImpl;
import dk.kb.spilsamlingen.api.v1.impl.ServiceApiServiceImpl;
import dk.kb.spilsamlingen.config.ServiceConfig;
import dk.kb.util.webservice.OpenApiResource;


public class Application_v1 extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        OpenApiResource.setConfig(ServiceConfig.getConfig());

        return new HashSet<>(Arrays.asList(
                JacksonJsonProvider.class,
                JacksonXMLProvider.class,
                KbSpilsamlingenApiServiceImpl.class,
                ServiceApiServiceImpl.class,
                OpenApiResource.class,
                dk.kb.util.webservice.exception.ServiceExceptionMapper.class
        ));
    }


}
