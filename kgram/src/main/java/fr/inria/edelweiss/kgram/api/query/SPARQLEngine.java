package fr.inria.edelweiss.kgram.api.query;

import fr.inria.edelweiss.kgram.core.Mappings;
import fr.inria.edelweiss.kgram.core.Query;

/**
 *
 * @author corby
 */
public interface SPARQLEngine {
    
    Mappings eval(Query q);
    
}