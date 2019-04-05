package ml.echelon133.services.graphstorage.graph;

import ml.echelon133.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public class GraphRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphRepository.class);

    private RedisTemplate<String, Graph<BigDecimal>> redisTemplate;
    private HashOperations<String, String, Graph<BigDecimal>> opsForHash;

    private final String GRAPH_KEY = "DirectedGraph";

    @Autowired
    public GraphRepository(RedisTemplate<String, Graph<BigDecimal>> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.opsForHash = redisTemplate.opsForHash();
        LOGGER.info("Instantiated GraphRepository");
    }

    public String save(Graph<BigDecimal> graph) {
        String graphId = UUID.randomUUID().toString();

        LOGGER.debug(String.format("Method save() tries to save graph %s with id %s", graph, graphId));
        opsForHash.put(GRAPH_KEY, graphId, graph);

        if (opsForHash.hasKey(GRAPH_KEY, graphId)) {
            LOGGER.debug(String.format("Graph %s was correctly saved with id %s", graph, graphId));
            return graphId;
        }

        LOGGER.debug(String.format("Graph %s was not saved correctly", graph));
        return null;
    }

    public Graph<BigDecimal> findById(String id) throws GraphNotFoundException {
        if (!opsForHash.hasKey(GRAPH_KEY, id)) {
            String msg = String.format("Graph with id %s not found", id);
            LOGGER.debug(msg);
            throw new GraphNotFoundException(msg);
        }

        LOGGER.debug(String.format("Graph with id %s found", id));
        return opsForHash.get(GRAPH_KEY, id);
    }

    // Only needed for setting mocks of HashOperations
    // There is no HashOperations bean, so there is no need to worry about runtime swaps of this dependency
    public void setOpsForHash(HashOperations<String, String, Graph<BigDecimal>> opsForHash) {
        this.opsForHash = opsForHash;
        LOGGER.debug(String.format("GraphRepository's HashOperations set manually to type: %s", opsForHash.getClass()));
    }
}
