package ml.echelon133.services.graphstorage;

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
