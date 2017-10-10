package fr.inria.corese.triple.term;

import fr.inria.acacia.corese.api.Computer;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.edelweiss.kgram.api.query.Environment;
import fr.inria.edelweiss.kgram.api.query.Producer;

/**
 *
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class NotEqual extends TermEval {
    
    public NotEqual(String name, Expression e1, Expression e2){
        super(name, e1, e2);
    }

    @Override
    public IDatatype eval(Computer eval, Binding b, Environment env, Producer p) {
        try {
            IDatatype dt1 = getArg(0).eval(eval, b, env, p);
            IDatatype dt2 = getArg(1).eval(eval, b, env, p);
            if (dt1 == null || dt2 == null) {
                return null;
            }
            return value(!dt1.equalsWE(dt2));
        } catch (CoreseDatatypeException ex) {
            return null;
        }
    }
    
}