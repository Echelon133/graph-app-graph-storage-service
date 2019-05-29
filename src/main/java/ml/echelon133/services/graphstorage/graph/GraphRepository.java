package ml.echelon133.services.graphstorage.graph;

import ml.echelon133.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class GraphRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphRepository.class);

    private RedisTemplate<String, String> vertexRedisTemplate;
    private RedisTemplate<String, Graph<BigDecimal>> graphRedisTemplate;

    private SetOperations<String, String> vertexOpsForSet;
    private HashOperations<String, String, Graph<BigDecimal>> graphOpsForHash;

    private final String GRAPH_KEY = "DirectedGraph";

    @Autowired
    public GraphRepository(RedisTemplate<String, String> vertexRedisTemplate,
                           RedisTemplate<String, Graph<BigDecimal>> graphRedisTemplate) {

        this.vertexRedisTemplate = vertexRedisTemplate;
        this.vertexOpsForSet = vertexRedisTemplate.opsForSet();

        this.graphRedisTemplate = graphRedisTemplate;
        this.graphOpsForHash = graphRedisTemplate.opsForHash();
        LOGGER.info("Instantiated GraphRepository");
    }

    public String save(Graph<BigDecimal> graph) {
        String graphId = UUID.randomUUID().toString();

        LOGGER.debug(String.format("Method save() tries to save graph %s with id %s", graph, graphId));
        graphOpsForHash.put(GRAPH_KEY, graphId, graph);

        if (graphOpsForHash.hasKey(GRAPH_KEY, graphId)) {
            LOGGER.debug(String.format("Graph %s was correctly saved with id %s", graph, graphId));
            return graphId;
        }

        LOGGER.debug(String.format("Graph %s was not saved correctly", graph));
        return null;
    }

    public Graph<BigDecimal> findById(String id) throws GraphNotFoundException {
        if (!graphOpsForHash.hasKey(GRAPH_KEY, id)) {
            String msg = String.format("Graph with id %s not found", id);
            LOGGER.debug(msg);
            throw new GraphNotFoundException(msg);
        }

        LOGGER.debug(String.format("Graph with id %s found", id));
        return graphOpsForHash.get(GRAPH_KEY, id);
    }


    // Only needed for setting mocks of HashOperations
    public void setGraphOpsForHash(HashOperations<String, String, Graph<BigDecimal>> opsForHash) {
        this.graphOpsForHash = opsForHash;
        LOGGER.debug(String.format("GraphRepository's HashOperations set manually to type: %s", opsForHash.getClass()));
    }

    // Only needed for setting mocks of SetOperations
    public void setVertexOpsForSet(SetOperations<String, String> opsForSet) {
        this.vertexOpsForSet = opsForSet;
        LOGGER.debug(String.format("GraphRepository's SetOperations set manually to type: %s", opsForSet.getClass()));
    }
}
