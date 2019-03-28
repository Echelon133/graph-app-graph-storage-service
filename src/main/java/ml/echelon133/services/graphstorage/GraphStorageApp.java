package ml.echelon133.services.graphstorage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ml.echelon133.graph.*;
import ml.echelon133.graph.json.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableDiscoveryClient
@EnableSwagger2
public class GraphStorageApp {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphStorageApp.class);

    @Bean
    public ObjectMapper objectMapper() {
        LOGGER.info("Started setup of ObjectMapper");
        SimpleModule module = new SimpleModule();
        ObjectMapper mapper = new ObjectMapper();

        JavaType vertexType = mapper.constructType(Vertex.class);
        JavaType edgeType = mapper.constructType(Edge.class);
        JavaType graphType = mapper.constructType(Graph.class);

        module.addSerializer(new VertexSerializer(vertexType));
        module.addSerializer(new EdgeSerializer(edgeType));
        module.addSerializer(new GraphSerializer(graphType));

        module.addDeserializer(Graph.class, new GraphDeserializer(graphType));

        mapper.registerModule(module);

        LOGGER.info("Finished setup of ObjectMapper");
        return mapper;
    }

    @Bean
    public Docket swaggerApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                    .apis(RequestHandlerSelectors.basePackage("ml.echelon133.services.graphstorage"))
                    .paths(PathSelectors.any())
                .build()
                .apiInfo(new ApiInfoBuilder()
                        .title("Graph Storage Service API")
                        .version("1.0")
                        .description("This is the documentation of Graph Storage Service")
                        .build());
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(GraphStorageApp.class).run(args);
    }
}
