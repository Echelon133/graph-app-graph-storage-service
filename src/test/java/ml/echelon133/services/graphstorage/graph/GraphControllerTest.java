package ml.echelon133.services.graphstorage.graph;

import com.fasterxml.jackson.databind.ObjectMapper;
import ml.echelon133.graph.Graph;
import ml.echelon133.graph.Vertex;
import ml.echelon133.graph.WeightedGraph;
import ml.echelon133.services.graphstorage.GraphStorageApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@RunWith(MockitoJUnitRunner.class)
public class GraphControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GraphRepository graphRepository;

    @InjectMocks
    private GraphController graphController;

    @InjectMocks
    private APIExceptionHandler exceptionHandler;

    private JacksonTester<Graph<BigDecimal>> jsonGraph;

    @Before
    public void before() {
        JacksonTester.initFields(this, GraphStorageApp.objectMapper());

        // Our mock controller does not use our custom ObjectMapper setup by default
        // We need to set up a message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(GraphStorageApp.objectMapper());

        mockMvc = MockMvcBuilders
                .standaloneSetup(graphController)
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    public void getGraphCorrectResponseWhenGraphNotFound() throws Exception {
        String searchedId = "asdf";
        String exceptionMsg = String.format("Graph with id %s not found", searchedId);

        // Given
        given(graphRepository.findById(searchedId)).willThrow(new GraphNotFoundException(exceptionMsg));

        // When
        MockHttpServletResponse response = mockMvc.perform(get("/api/graphs/" + searchedId)
                .accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getContentAsString()).contains(exceptionMsg);
    }

    @Test
    public void getGraphCorrectResponseWhenGraphFound() throws Exception {
        String searchedId = "asdf";

        // Test graph
        Graph<BigDecimal> graph = new WeightedGraph<>();
        graph.addVertex(new Vertex<>("v1"));
        graph.addVertex(new Vertex<>("v2"));
        graph.addVertex(new Vertex<>("v3"));
        graph.addEdge(graph.findVertex("v1"), graph.findVertex("v2"), new BigDecimal(5));
        graph.addEdge(graph.findVertex("v1"), graph.findVertex("v3"), new BigDecimal(15));
        graph.addEdge(graph.findVertex("v2"), graph.findVertex("v3"), new BigDecimal(25));

        JsonContent<Graph<BigDecimal>> expectedGraphJsonContent = jsonGraph.write(graph);

        // Given
        given(graphRepository.findById(eq(searchedId))).willReturn(graph);

        // When
        MockHttpServletResponse response = mockMvc.perform(get("/api/graphs/" + searchedId)
                .accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getContentAsString()).isEqualTo(expectedGraphJsonContent.getJson());
    }
}
