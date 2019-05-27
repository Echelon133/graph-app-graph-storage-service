package ml.echelon133.services.graphstorage.graph;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@RunWith(MockitoJUnitRunner.class)
public class GraphControllerTest {

    private MockMvc mockMvc;

    private Integer maxEdgesCount = 3;

    @Mock
    private GraphRepository graphRepository;

    @InjectMocks
    private GraphController graphController;

    @InjectMocks
    private APIExceptionHandler exceptionHandler;

    private JacksonTester<Graph<BigDecimal>> jsonGraph;

    @Before
    public void before() {
        JacksonTester.initFields(this, GraphStorageApp.objectMapper(maxEdgesCount));

        // Our mock controller does not use our custom ObjectMapper setup by default
        // We need to set up a message converter
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(GraphStorageApp.objectMapper(maxEdgesCount));

        mockMvc = MockMvcBuilders
                .standaloneSetup(graphController)
                .setControllerAdvice(exceptionHandler)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    public void getGraphRespondsCorrectlyWhenGraphNotFound() throws Exception {
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
    public void getGraphRespondsCorrectlyWhenGraphFound() throws Exception {
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

    @Test
    public void addGraphRespondsCorrectlyWhenPayloadNumberOfEdgesEqualToMaxEdgesCount() throws Exception {
        String graphId = "asdf-asdf-asdf-asdf";

        // Test graph
        Graph<BigDecimal> graph = new WeightedGraph<>();
        graph.addVertex(new Vertex<>("v1"));
        graph.addVertex(new Vertex<>("v2"));

        for (int i = 0; i < maxEdgesCount; i++) {
            graph.addEdge(graph.findVertex("v1"), graph.findVertex("v2"), new BigDecimal(5));
        }

        JsonContent<Graph<BigDecimal>> graphPayload = jsonGraph.write(graph);

        // Given
        given(graphRepository.save(any())).willReturn(graphId);

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload.getJson())
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.getContentAsString()).contains(graphId);
    }

    @Test
    public void addGraphPayloadNumberOfEdgesExceedsMaxEdgesCountHandledCorrectly() throws Exception {
        String expectedMessage = String.format("Cannot accept graphs that contain more than %d edges", maxEdgesCount);

        // Test graph
        Graph<BigDecimal> graph = new WeightedGraph<>();
        graph.addVertex(new Vertex<>("v1"));
        graph.addVertex(new Vertex<>("v2"));

        for (int i = 0; i <= maxEdgesCount; i++) {
            graph.addEdge(graph.findVertex("v1"), graph.findVertex("v2"), new BigDecimal(5));
        }

        JsonContent<Graph<BigDecimal>> graphPayload = jsonGraph.write(graph);

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload.getJson())
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadMissingVertexesNodeHandledCorrectly() throws Exception {
        String expectedMessage = "Missing 'vertexes' JSON node.";

        String graphPayload = "{\"edges\":[]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadMissingEdgesNodeHandledCorrectly() throws Exception {
        String expectedMessage = "Missing 'edges' JSON node.";

        String graphPayload = "{\"vertexes\":[]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongVertexesTypeHandledCorrectly() throws Exception {
        String expectedMessage = "'vertexes' is not an array node.";

        String graphPayload = "{\"vertexes\": \"array expected here\", \"edges\": []}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongEdgesTypeHandledCorrectly() throws Exception {
        String expectedMessage = "'edges' is not an array node.";

        String graphPayload = "{\"vertexes\": [], \"edges\": \"array expected here\"}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongVertexTypeHandledCorrectly() throws Exception {
        String expectedMessage = "Vertex element in 'vertexes' is not textual";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\", \"v3\", 4], \"edges\": []}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongEdgeTypeHandledCorrectly() throws Exception {
        String expectedMessage = "Edge element in 'edges' is not an object";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [1]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadNegativeEdgeWeightHandledCorrectly() throws Exception {
        String expectedMessage = "Edge weight cannot be negative";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : \"v1\", \"destination\" : \"v2\", \"weight\" : -20}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadEdgeMissingSourceHandledCorrectly() throws Exception {
        String expectedMessage = "Edge object does not contain 'source' field";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"destination\" : \"v2\", \"weight\" : 20}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadEdgeMissingDestinationHandledCorrectly() throws Exception {
        String expectedMessage = "Edge object does not contain 'destination' field";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : \"v1\", \"weight\" : 20}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadEdgeMissingWeightHandledCorrectly() throws Exception {
        String expectedMessage =  "Edge object does not contain 'weight' field";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : \"v1\", \"destination\" : \"v2\"}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongEdgeSourceTypeHandledCorrectly() throws Exception {
        String expectedMessage = "Source vertex in Edge is not textual";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : 1010, \"destination\" : \"v2\", \"weight\" : 20}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadWrongEdgeDestinationTypeHandledCorrectly() throws Exception {
        String expectedMessage = "Destination vertex in Edge is not textual";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : \"v1\", \"destination\" : 1010, \"weight\" : 20}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadEdgeWeightNotNumericHandledCorrectly() throws Exception {
        String expectedMessage = "Weight cannot be deserialized as BigDecimal";

        String graphPayload = "{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [{\"source\" : \"v1\", \"destination\" : \"v2\", \"weight\" : \"asdf\"}]}";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphPayloadInvalidVertexNameReferenceHandledCorrectly() throws Exception {
        String invalidReferenceEdge = "{\"source\":\"v1\",\"destination\":\"v3\",\"weight\":20}";
        String graphPayload = String.format("{\"vertexes\": [\"v1\", \"v2\"], \"edges\": [%s]}", invalidReferenceEdge);

        String expectedMessage = "references a vertex that is not present in 'vertexes'";

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }

    @Test
    public void addGraphDuplicateVertexNameHandledCorrectly() throws Exception {
        String vertexName = "v1";
        String expectedMessage = String.format("Vertex with name %s already belongs to the graph", vertexName);

        String graphPayload = String.format("{\"vertexes\": [\"%s\", \"%s\"], \"edges\": []}", vertexName, vertexName);

        // When
        MockHttpServletResponse response = mockMvc.perform(post("/api/graphs/")
                .accept(MediaType.APPLICATION_JSON)
                .content(graphPayload)
                .contentType(MediaType.APPLICATION_JSON)).andReturn().getResponse();

        // Then
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString()).contains(expectedMessage);
    }
}
