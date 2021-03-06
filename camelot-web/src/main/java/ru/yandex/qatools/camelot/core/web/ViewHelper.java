package ru.yandex.qatools.camelot.core.web;


import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import de.neuland.jade4j.Jade4J;
import de.neuland.jade4j.parser.Parser;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.JadeTemplate;
import de.neuland.jade4j.template.ReaderTemplateLoader;
import jodd.util.Wildcard;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import ru.yandex.qatools.camelot.config.Plugin;
import ru.yandex.qatools.camelot.config.PluginContext;
import ru.yandex.qatools.camelot.core.ProcessingEngine;
import ru.yandex.qatools.camelot.core.WebfrontEngine;
import ru.yandex.qatools.camelot.core.web.jackson.JsonSerializer;

import javax.xml.bind.JAXBException;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
@Component
public class ViewHelper {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final WebfrontEngine pluginsService;
    final ApplicationContext context;
    final Map<String, Map<String, Object>> pluginRenderAttrs = new HashMap<String, Map<String, Object>>();

    @Autowired
    ProcessingEngine processing;

    @Autowired
    JsonSerializer jsonSerializer;

    @Autowired
    public ViewHelper(WebfrontEngine pluginsEngine, ApplicationContext context) {
        this.pluginsService = pluginsEngine;
        this.context = context;
    }

    /**
     * Renders the plugin's dashboard
     */
    public String renderPluginDashboard(String pluginId) {
        final PluginContext config = pluginsService.getPluginContext(pluginId);
        return renderPluginTemplate(pluginId, config.getDashboardPath());
    }

    /**
     * Renders the plugin's widget
     */
    public String renderPluginWidgetContent(String pluginId) {
        final PluginContext config = pluginsService.getPluginContext(pluginId);
        return renderPluginTemplate(pluginId, config.getWidgetPath());
    }

    /**
     * Print plugins data serialized to json
     */
    public String printPluginsData() throws IOException, JAXBException {
        Map<String, Plugin> plugins = processing.getPluginsMap();
        return jsonSerializer.toJson(plugins);
    }

    /**
     * Returns last part of the string splitted by "."
     */
    public String pluginTitle(String pluginId) {
        final String[] parts = pluginId.split("\\.");
        return parts[parts.length - 1];
    }

    /** ------------------------------------------------- **/

    /**
     * Renders the plugin template file with context
     */
    private String renderPluginTemplate(String pluginId, String templatePath) {
        final PluginContext context = pluginsService.getPluginContext(pluginId);
        if (!isEmpty(templatePath)) {
            try {
                return renderSource(
                        templatePath,
                        pluginsService.getLoader().getResourceAsStream(context.getSource(), templatePath),
                        getPluginRenderAttrs(context)
                );
            } catch (Exception e) {
                logger.error("Failed to render jade template: ", e);
                return "Failed to render plugin template: " + e.getMessage();
            }
        }
        return "No such template found for plugin '" + pluginId + "' (" + templatePath + ")!";
    }

    /**
     * Returns and caches the plugin dashboard attributes
     */
    public Map<String, Object> getPluginRenderAttrs(PluginContext config) {
        final String pluginId = config.getId();
        if (!pluginRenderAttrs.containsKey(pluginId)) {
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("repo", config.getRepository());
            attrs.put("storage", config.getStorage());
            attrs.put("id", pluginId);
            attrs.put("plugins", pluginsService.getInterop());
            if (context instanceof WebApplicationContext) {
                attrs.put("contextPath", ((WebApplicationContext) context).getServletContext().getContextPath());
            }
            attrs.put("helper", new PluginViewHelper(pluginId, this));
            pluginRenderAttrs.put(pluginId, attrs);
        }
        return pluginRenderAttrs.get(pluginId);
    }

    /**
     * Renders the resource via one of the registered renderers
     */
    public String renderSource(String fileName, InputStream resource, Map<String, Object> attrs) throws IOException {
        if (Wildcard.match(fileName, "*.jade")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource));
            final ReaderTemplateLoader loader = new ReaderTemplateLoader(reader, fileName);
            Parser parser = new Parser(fileName, loader);
            Node root = parser.parse();
            JadeTemplate template = new JadeTemplate();
            template.setTemplateLoader(loader);
            template.setRootNode(root);
            return Jade4J.render(template, attrs);
        } else if (Wildcard.match(fileName, "*.mustache")) {
            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile(new InputStreamReader(resource), fileName);
            final StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, attrs);
            return stringWriter.toString();
        } else if (Wildcard.match(fileName, "*.html")) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(resource, writer, "UTF-8");
            return writer.toString();
        }
        return "No template renderer found for " + resource;
    }


}
