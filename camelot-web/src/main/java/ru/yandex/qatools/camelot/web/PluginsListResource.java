package ru.yandex.qatools.camelot.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.qatools.camelot.config.Plugin;
import ru.yandex.qatools.camelot.core.impl.PluginTree;
import ru.yandex.qatools.camelot.core.ProcessingEngine;
import ru.yandex.qatools.camelot.core.WebfrontEngine;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.io.IOException;
import java.util.Map;

@Component
@Path("/pluginslist")
public class PluginsListResource extends BasicViewResource {

    @Autowired
    WebfrontEngine webfront;
    @Autowired
    ProcessingEngine processing;

    public PluginTree getPluginTree() {
        return webfront.getPluginTree();
    }

    public WebfrontEngine webfront() {
        return webfront;
    }

    public ProcessingEngine processing() {
        return processing;
    }

    @GET
    @Path("get")
    @Produces({APP_JSON})
    public Map<String, Plugin> asJson(@Context ServletContext context, @Context HttpServletRequest request) throws IOException {
        return processing.getPluginsMap();
    }

    @Override
    public Object getTitle() {
        return "Plugins list";
    }
}
