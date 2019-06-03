package ml.echelon133.services.graphstorage.graph;

import ml.echelon133.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/graphs")
public class GraphController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphController.class);

    private GraphRepository graphRepository;

    @Autowired
    public GraphController(GraphRepository graphRepository) {
        this.graphRepository = graphRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Graph<BigDecimal>> getGraph(@PathVariable String id) throws Exception {
        LOGGER.debug(String.format("getGraph with id %s from the database", id));
        Graph<BigDecimal> graph = graphRepository.findById(id);

        LOGGER.debug(String.format("Return response with a serialized graph that has an id %s", id));
        return new ResponseEntity<>(graph, HttpStatus.OK);
    }

    @GetMapping("/{id}/vertexes")
    public ResponseEntity<Map<String, Boolean>> checkGraphVertexStatus(@PathVariable String id, @RequestParam String name) throws Exception {
        LOGGER.debug(String.format("checkGraphVertexStatus of vertex %s in a graph with id %s", name, id));

        Boolean contains = graphRepository.graphHasVertex(id, name);

        Map<String, Boolean> response = Collections.singletonMap("contains", contains);
        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    @PostMapping("/")
    public ResponseEntity<Map<String, String>> addGraph(@RequestBody Graph<BigDecimal> graph) throws Exception {
        Map<String, String> response = new HashMap<>();

        LOGGER.debug(String.format("@RequestBody graph deserialized correctly as %s. Attempting saving it.", graph));
        String id = graphRepository.save(graph);

        response.put("id", id);
        LOGGER.debug(String.format("Returning response with an id %s of the graph that was just serialized", id));
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
