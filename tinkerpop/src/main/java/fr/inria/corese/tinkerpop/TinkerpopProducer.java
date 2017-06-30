/*
 *  Copyright Inria 2016
 */
package fr.inria.corese.tinkerpop;

import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.Metadata;
import fr.inria.acacia.corese.triple.parser.Variable;
import fr.inria.corese.rdftograph.driver.GdbDriver;
import fr.inria.edelweiss.kgenv.parser.NodeImpl;
import fr.inria.edelweiss.kgram.api.core.DatatypeValue;
import fr.inria.edelweiss.kgram.api.core.Edge;
import fr.inria.edelweiss.kgram.api.core.Entity;
import fr.inria.edelweiss.kgram.api.core.Node;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.core.Exp;
import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Mapping;
import fr.inria.edelweiss.kgram.core.Query;
import fr.inria.edelweiss.kgraph.core.producer.DataProducer;
import fr.inria.edelweiss.kgraph.core.Graph;
import fr.inria.edelweiss.kgraph.query.ProducerImpl;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.Iterator;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 *
 * @author edemairy
 */
public class TinkerpopProducer extends ProducerImpl {

	private final Logger LOGGER = LogManager.getLogger(TinkerpopProducer.class.getName());
	private GdbDriver databaseDriver;
	private TinkerpopGraph graph;
	private DataProducer ei;

	public TinkerpopProducer(TinkerpopGraph graph, GdbDriver databaseDriver) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		super(graph);
		this.databaseDriver = databaseDriver;
		this.graph = graph;
	}

	/**
	 * @test annotation exploits relevant filters for query edge
	 */
	@Override
	public Iterable<Entity> getEdges(Node gNode, List<Node> from, Edge qEdge, Environment env) {
		ASTQuery ast = (ASTQuery) env.getQuery().getAST();
		if (ast.hasMetadata(Metadata.TEST)) {
			return getEdgesNew(gNode, from, qEdge, env);
		} else {
			return getEdgesSave(gNode, from, qEdge, env);
		}

	}

	/**
	 * Generate Entity iterable Exploit variable value, constant in edge and
	 * filters that are relevant for current edge filter (?o = "test")
	 * generates Tinkerpop predicate P.eq("test") This is done in the Driver
	 * Values are passed as DatatypeValue in order to distinguish numeric,
	 * string and boolean values
	 *
	 * @param gNode
	 * @param from
	 * @param qEdge
	 * @param env
	 * @return
	 */
	public Iterable<Entity> getEdgesNew(Node gNode, List<Node> from, Edge qEdge, Environment env) {
		Exp exp = env.getExp();
		Query q = env.getQuery();
		ASTQuery ast = (ASTQuery) q.getAST();
		boolean isDebug = q.isDebug();
		exp.setDebug(isDebug);
		Node subject = qEdge.getNode(0);
		Node object = qEdge.getNode(1);
		Node predicate = getPredicate(qEdge);

		Function<GraphTraversalSource, GraphTraversal<? extends Element, ? extends Element>> filter;

		DatatypeValue s = updateVariable(exp, Exp.SUBJECT, subject, env, isDebug);
		DatatypeValue p = updateVariable(exp, Exp.PREDICATE, predicate, env, isDebug);
		DatatypeValue o = updateVariable(exp, Exp.OBJECT, object, env, isDebug);
		DatatypeValue g = updateVariable(exp, Exp.GRAPH_NAME, gNode, env, isDebug);

		filter = databaseDriver.getFilter(exp, s, p, o, g);
		if (q.isDebug()) {
			System.out.println("TK: " + " " + s + " " + p + " " + o);
		}
		Iterable<Entity> result = graph.getEdges(filter);
		return result;
	}

	@Override
	public Mappings getMappings(Node gNode, List<Node> from, Exp exp, Environment env) {
		exp.setDebug(env.getQuery().isDebug());
		Function<GraphTraversalSource, GraphTraversal<? extends Element, Map<String, Vertex>>> filter
			= databaseDriver.getFilter(exp);
		Iterator<Map<String, Vertex>> vmap = graph.getMaps(filter);
		Mappings map = Mappings.create(env.getQuery());
		while (vmap.hasNext()) {
			Mapping m = process(vmap.next());
			map.add(m);
			if (env.getQuery().isDebug()) {
				System.out.println(m);
			}
		}
		return map;
	}

	Mapping process(Map<String, Vertex> m) {
		ArrayList<Node> ql = new ArrayList<Node>();
		ArrayList<Node> tl = new ArrayList<Node>();
		for (String s : m.keySet()) {
			ql.add(NodeImpl.createVariable(s));
			tl.add(graph.getNode(m.get(s)));
		}
		return Mapping.create(ql, tl);
	}

	private DatatypeValue updateVariable(Exp exp, int rank, Node node, Environment env, boolean isDebug) {
		if (node == null) {
			return null;
		}
		DatatypeValue result = null;
		if (node.isVariable()) {
			Node value = env.getNode(node.getLabel());
			if (value != null) {
				// var bound
				result = value.getDatatypeValue();
			}
		} else {
			result = node.getDatatypeValue();
		}
		return result;
	}

	/**
	 * Generate a fake variable Node when property = TopLevel property
	 *
	 * @param e
	 * @return
	 */
	Node getPredicate(Edge e) {
		Node predicate = e.getEdgeVariable();
		if (predicate != null) {
			return predicate;
		}
		Node property = e.getEdgeNode();
		if (property.getLabel().equals(Graph.TOPREL)) {
			return new NodeImpl(new Variable("?_tmp_var_"));
		}
		return property;
	}

	/**
	 * Property Path n = 0 : focusNode is subject n = 1 : focusNode is
	 * object
	 */
	@Override
	public Iterable<Entity> getEdges(Node gNode, Node sNode, List<Node> from,
		Node predicate, Node focusNode, Node objectNode, int n) {

		DatatypeValue s = (n == 0) ? focusNode.getDatatypeValue() : null;
		DatatypeValue o = (n == 1) ? focusNode.getDatatypeValue() : null;

		Function<GraphTraversalSource, GraphTraversal<? extends Element, ? extends Element>> filter;
		filter = databaseDriver.getFilter(null, s, predicate.getDatatypeValue(), o, null);
		Iterable<Entity> result = graph.getEdges(filter);
		return result;
	}

	@Override
	public void close() {
		graph.close();
	}

	/**
	 * @return An iterable
	 * @param gNode @TODO Not used for the moment
	 * @param from @TODO Not used for the moment
	 * @param qEdge Requested edge.
	 * @param env Provided values that can set the values for all the Nodes
	 * of the request.
	 */
	//@Override
	public Iterable<Entity> getEdgesSave(Node gNode, List<Node> from, Edge qEdge, Environment env) {
		Exp exp = env.getExp();
		Query q = env.getQuery();
		boolean isDebug = q.isDebug();
		Node subject = qEdge.getNode(0);
		Node object = qEdge.getNode(1);
		Node predicate = getPredicate(qEdge);

		Function<GraphTraversalSource, GraphTraversal<? extends org.apache.tinkerpop.gremlin.structure.Element, ? extends org.apache.tinkerpop.gremlin.structure.Element>> filter;
		StringBuilder key = new StringBuilder();

		String g = (gNode == null) ? "" : gNode.getLabel();
		key.append((gNode == null) || (gNode.isVariable()) ? "?g" : "G");

		String s = updateVariableSave(exp, Exp.SUBJECT, subject, env, key, isDebug);
		String p = updateVariableSave(exp, Exp.PREDICATE, predicate, env, key, isDebug);
		String o = updateVariableSave(exp, Exp.OBJECT, object, env, key, isDebug);
		//LOGGER.trace("in case " + key.toString());       
		filter = databaseDriver.getFilter(exp, key.toString(), s, p, o, g);
		if (q.isDebug()) {
			System.out.println("TK: " + key + " " + s + " " + p + " " + o);
		}
		Iterable<Entity> result = graph.getEdges(filter);
		return result;
	}

	String free(int rank) {
		switch (rank) {
			case Exp.SUBJECT:
				return "?s";
			case Exp.OBJECT:
				return "?o";
			case Exp.PREDICATE:
				return "?p";
		}
		return "";
	}

	String bound(int rank) {
		switch (rank) {
			case Exp.SUBJECT:
				return "S";
			case Exp.OBJECT:
				return "O";
			case Exp.PREDICATE:
				return "P";
		}
		return "";
	}

	private String updateVariableSave(Exp exp, int rank, Node node, Environment env, StringBuilder key, boolean isDebug) {
		String result = "";
		if (node.isVariable()) {
			if (env.getNode(node.getLabel()) != null) { // Variable was given free in the request, but its value is fixed by env.
				// var bound
				key.append(bound(rank));
				result = env.getNode(node.getLabel()).getLabel();
			} else {
				key.append(free(rank));
			}
		} else {
			key.append(bound(rank));
			result = node.getLabel();
		}
		return result;
	}

}
