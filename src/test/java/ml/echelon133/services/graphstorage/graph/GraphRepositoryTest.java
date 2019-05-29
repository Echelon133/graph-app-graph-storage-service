package ml.echelon133.services.graphstorage.graph;

import ml.echelon133.graph.Graph;
import ml.echelon133.graph.WeightedGraph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.AssertionsForClassTypes.*;

@RunWith(MockitoJUnitRunner.class)
public class GraphRepositoryTest {

    @Mock
    private RedisTemplate<String, Graph<BigDecimal>> redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private HashOperations<String, String, Graph<BigDecimal>> hashOperations;

    @InjectMocks
    private GraphRepository graphRepository;

    @Before
    public void before() {
        /*
        Setting a mock manually because:
              given(redisTemplate.opsForHash()).willReturn(hashOperations);
        does not allow for using the actual type parameters of HashOperations
        GraphRepository uses <String, String, Graph<BigDecimal> and this demands <String, Object, Object>)
        There is probably another way, but this is a quick and simple solution
        */
        graphRepository.setGraphOpsForHash(hashOperations);

        graphRepository.setVertexOpsForSet(setOperations);
    }

    @Test
    public void saveReturnsIdAfterSuccessfulSave() {
        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), anyString())).willReturn(true);

        // When
        String response = graphRepository.save(new WeightedGraph<>());

        // Then
        assertThat(response).isNotNull();
    }

    @Test
    public void saveReturnsNullAfterFailedSave() {
        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), anyString())).willReturn(false);

        // When
        String response = graphRepository.save(new WeightedGraph<>());

        // Then
        assertThat(response).isNull();
    }

    @Test
    public void findByIdThrowsGraphNotFoundExceptionWhenGraphIsNotFound() {
        String searchedId = "asdf";

        String expectedMsg = "Graph with id asdf not found";
        String receivedMsg = "";

        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), eq(searchedId))).willReturn(false);

        // When
        try {
            graphRepository.findById(searchedId);
        } catch (GraphNotFoundException ex) {
            receivedMsg = ex.getMessage();
        }

        // Then
        assertThat(receivedMsg).isEqualTo(expectedMsg);
    }

    @Test
    public void findByIdReturnsGraphWhenFound() throws Exception {
        Graph<BigDecimal> graph = new WeightedGraph<>();
        String searchedId = "asdf";

        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), eq(searchedId))).willReturn(true);
        given(hashOperations.get(eq("DirectedGraph"), eq(searchedId))).willReturn(graph);

        // When
        Graph<BigDecimal> retrievedGraph = graphRepository.findById(searchedId);


        // Then
        assertThat(retrievedGraph).isEqualTo(graph);
    }

    @Test
    public void graphHasVertexThrowsGraphNotFoundExceptionWhenGraphIsNotFound() {
        String searchedId = "test";

        String expectedMsg = "Graph with id test not found";
        String receivedMsg = "";

        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), eq(searchedId))).willReturn(false);

        // When
        try {
            graphRepository.graphHasVertex(searchedId, "testVertex");
        } catch (GraphNotFoundException ex) {
            receivedMsg = ex.getMessage();
        }

        // Then
        assertThat(receivedMsg).isEqualTo(expectedMsg);
    }

    @Test
    public void graphHasVertexReturnsTrueWhenVertexIsMember() throws Exception {
        String graphId = "test";
        String vertexName = "test";

        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), eq(graphId))).willReturn(true);
        given(setOperations.isMember(eq(graphId), eq(vertexName))).willReturn(true);

        // When
        Boolean result = graphRepository.graphHasVertex(graphId, vertexName);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    public void graphHasVertexReturnsFalseWhenVertexIsNotMember() throws Exception {
        String graphId = "test";
        String vertexName = "test";

        // Given
        given(hashOperations.hasKey(eq("DirectedGraph"), eq(graphId))).willReturn(true);
        given(setOperations.isMember(eq(graphId), eq(vertexName))).willReturn(false);

        // When
        Boolean result = graphRepository.graphHasVertex(graphId, vertexName);

        // Then
        assertThat(result).isFalse();
    }
}
