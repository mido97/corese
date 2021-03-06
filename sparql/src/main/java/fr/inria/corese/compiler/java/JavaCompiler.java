package fr.inria.corese.compiler.java;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.triple.parser.ASTExtension;
import fr.inria.acacia.corese.triple.parser.ASTQuery;
import fr.inria.acacia.corese.triple.parser.Constant;
import fr.inria.acacia.corese.triple.parser.Expression;
import fr.inria.corese.triple.function.script.ForLoop;
import fr.inria.corese.triple.function.script.Function;
import fr.inria.corese.triple.function.script.Let;
import fr.inria.acacia.corese.triple.parser.Metadata;
import fr.inria.acacia.corese.triple.parser.NSManager;
import fr.inria.acacia.corese.triple.parser.Processor;
import fr.inria.corese.triple.function.script.Statement;
import fr.inria.acacia.corese.triple.parser.Term;
import fr.inria.acacia.corese.triple.parser.Variable;
import fr.inria.edelweiss.kgram.api.core.ExprType;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Java Compiler for LDScript
 * Take an AST as input and compile the LDScript function definitions in Java
 * 
 * @author Olivier Corby, Wimmics INRIA I3S, 2017
 *
 */
public class JavaCompiler {
  
    private static Logger logger = LogManager.getLogger(JavaCompiler.class);
    static final String NL = System.getProperty("line.separator");
    String path =
            "/user/corby/home/NetBeansProjects/corese-github/kgtool/src/main/java/fr/inria/corese/extension/";
    static final String SPACE = " ";
    static final int STEP = 2;
    static final String IDATATYPE = "IDatatype";
    static final String PROXY_PACKAGE = "fr.inria.edelweiss.kgenv.eval.ProxyImpl";
    
    
    
    public static final String VAR_EXIST = "?_b";
    public String pack = "fr.inria.corese.extension";
    
    
    int margin = 0;
    int count = 0;
    String name = "Extension";
    
    StringBuilder sb;
    Header head;
    Datatype dtc;
    ASTQuery ast;
    // current function processing
    private Function function;
    // stack of bound variables (function parameter, let)
    Stack stack;
    
    HashMap<String, Boolean> skip;
    HashMap<String, String> functionName;
    

    public JavaCompiler() {
        sb = new StringBuilder();
        dtc = new Datatype();
        stack = new Stack();
        head = new Header(this);
        skip = new HashMap<String, Boolean>();
        functionName = new HashMap<>();
        init();
    }
    
    /**
     * 
     * target Java class name 
     */
     public JavaCompiler(String name) {
         this();
         record(name);
     }
     
     /**
      * name = DataShape or fr.inria.corese.extension.DataShape
      * extract package name if any
      */
     void record(String name){
         this.name = name;
         int index = name.lastIndexOf(".");
         if (index != -1){
             pack = name.substring(0, index);
             this.name = name.substring(index+1);
         }
         System.out.println("package: " + this.pack);
         System.out.println("class: " + this.name);
     }

     void init(){
        skip.put(NSManager.SHAPE + "class", true);
        skip.put(NSManager.STL + "default", true);
        skip.put(NSManager.STL + "aggregate", true);
        
        functionName.put("isURI",       "isURINode");
        functionName.put("isBlank",     "isBlankNode");
        functionName.put("isLiteral",   "isLiteralNode");
        
        functionName.put(Processor.XT_GEN_GET,     "GetGen.gget");
        functionName.put(Processor.XT_GET,         "Get.get");
        
        functionName.put(Processor.XT_LIST,         "DatatypeMap.newList");
        functionName.put(Processor.XT_SIZE,         "DatatypeMap.size");
        functionName.put(Processor.XT_MEMBER,       "DatatypeMap.member");
        functionName.put(Processor.XT_FIRST,        "DatatypeMap.first");
        functionName.put(Processor.XT_REST,         "DatatypeMap.rest");
        functionName.put(Processor.XT_ADD,          "DatatypeMap.add");
        functionName.put(Processor.XT_CONS,         "DatatypeMap.cons");
        functionName.put(Processor.XT_REVERSE,      "DatatypeMap.reverse");
        functionName.put(Processor.XT_MERGE,        "DatatypeMap.merge");
       
        functionName.put(Processor.STRLEN,           "DatatypeMap.strlen");
        functionName.put(Processor.STRDT,           "DatatypeMap.newInstance");
    }
    
    boolean skip(String name){
        Boolean b = skip.get(name);
        return b != null && b;
    }
    
    @Override
    public String toString() {
        return sb.toString();
    }

    /**
     * Main function to compile  AST functions
     */
    public JavaCompiler toJava(ASTQuery ast) throws IOException {
        this.ast = ast;
        path(ast);
        head.process(pack, name);
        toJava(ast.getDefine());
        toJava(ast.getDefineLambda());
        trailer();
        return this;
    }

    public void toJava(ASTExtension ext) throws IOException {
        for (ASTExtension.ASTFunMap m : ext.getMaps()) {
            for (Function exp : m.values()) {
                if (! exp.hasMetadata(Metadata.SKIP)){ 
                    compile(exp);
                    append(NL);
                }
            }
        }
    }
    
    void path(ASTQuery ast){
        if (ast.hasMetadata(Metadata.PATH)){
            path = ast.getMetadata().getValue(Metadata.PATH);
        }
        System.out.println("path: " + path);
    }
    
    public void write() throws IOException {
         write(path);
    }

    public void write(String path) throws IOException {
        FileWriter fw = new FileWriter(String.format("%s%s.java", path, name));
        fw.write(head.getStringBuilder().toString());
        fw.write(dtc.getStringBuilder().toString());
        fw.write(NL);
        fw.write(sb.toString());
        fw.flush();
        fw.close();
    }

    

    void trailer() {
        append("}");
        nl();
    }

    
    
    /**
     * Compile one function
     */
    void compile(Function exp) {
        stack.push(exp);
        functionDeclaration(exp);
        append(" {");
        incrnl();
        Rewrite rw = new Rewrite(ast);
        Expression body = rw.process(exp.getBody());
        toJava(body);
        stack.pop(exp);
        pv(body);
        decrnl();
        append("}");
        nl();
    }

    public void toJava(Expression exp) {
        exp.toJava(this);
    }

    public void toJava(Variable var) {
        append(JavaCompiler.this.name(var));
    }
  
    public void toJava(Constant cst) {
        append(dtc.toJava(cst.getDatatypeValue()));
    }

    public void toJava(Term term) {
        if (term.isExist()){
            new Pattern(this).exist(term);
        }
        else if (term.getName().equals(Processor.SEQUENCE)) {
            sequence(term);
        } else if (term.isFunction()) {
            functionCall(term);
        } else {
            term(term);
        }
    }
    
    public void toJava(Function fun){
        append(dtc.string(name(fun)));
    }
    
    public void toJava(Statement term) {
        switch (term.oper()) {
            case ExprType.LET:
                let(term.getLet());
                return;
                
            case ExprType.FOR:
                loop(term.getFor());
                return;
        }
    }
    
   
    String name(Variable var){
        return var.getSimpleName();
    }
  
    
    void functionDeclaration(Function fun) {
        Term term = fun.getSignature();
        append("public").append(SPACE).append(IDATATYPE).append(SPACE);
        append(name(fun)).append("(");
        int i = 0;
        for (Expression exp : term.getArgs()) {
            append(IDATATYPE).append(SPACE);
            Variable var = exp.getVariable();
            toJava(var);
            if (i++ < term.arity() - 1) {
                append(", ");
            }
        }
        append(")");
    }
    
    String name(Function fun){
        if (fun.isLambda()){
            return "xt" + fun.getFunction().javaName().replace("-", "_");
        }
        return fun.getFunction().javaName();
    }

     void nl() {
        append(NL);
        for (int i = 0; i < margin; i++) {
            append(SPACE);
        }
    }

    void incr() {
        margin += STEP;
    }

    void incrnl() {
        incr();
        nl();
    }

    void decrnl() {
        decr();
        nl();
    }

    void decr() {
        margin -= STEP;
    }
    
    
    void pv(Expression exp) {
        if (isPV(exp)) {
            pv();
        }
    }
    
    void pv(){
        append(";");
    }

    boolean isPV(Expression exp) {
        if (!exp.isTerm()) {
            return false;
        }
        switch (exp.oper()) {
            case ExprType.IF:
            case ExprType.FOR:
            case ExprType.LET:
            case ExprType.SEQUENCE:

                return false;
        }
        return true;
    }

    StringBuilder append(String str) {
        sb.append(str);
        return sb;
    }
    
    void loop(ForLoop term){
        append("for (IDatatype ");
        toJava(term.getVariable());
        append(" : ");
        toJava(term.getDefinition());
        append(".getValueList()) {");
        incrnl();
        stack.push(term);
        toJava(term.getBody());
        stack.pop(term);
        pv(term.getBody());
        decrnl();
        append("}");
    }

    void let(Let term) {
        if (! stack.isBound(term.getVariable())){
            append(IDATATYPE).append(SPACE);
        }
        toJava(term.getVariable());
        append(" = ");
        toJava(term.getDefinition());
        pv();
        nl();
        stack.push(term);
        toJava(term.getBody());
        stack.pop(term);
        pv(term.getBody());
    }

    void term(Term term) {
        switch (term.oper()) {
            case ExprType.NOT:
                not(term);
                return;
            case ExprType.AND:
                and(term);
                return;
            case ExprType.OR:
                or(term);
                return;

            case ExprType.IN:
                in(term);
                return;

        }

        toJava(term.getArg(0));
        append(".");
        append(getTermName(term)).append("(");
        toJava(term.getArg(1));
        append(")");
    }

    void in(Term term) {
        append("in(");
        toJava(term.getArg(0));
        append(", DatatypeMap.newList(");
        int i = 0;
        int size = term.getArg(1).arity() - 1;
        for (Expression exp : term.getArg(1).getArgs()) {
            toJava(exp);
            if (i++ < size) {
                append(", ");
            }
        }
        append("))");
    }

    void and(Term term) {
        append("and(");
        toJava(term.getArg(0));
        append(", ");
        toJava(term.getArg(1));
        append(")");
    }

    void or(Term term) {
        append("or(");
        toJava(term.getArg(0));
        append(", ");
        toJava(term.getArg(1));
        append(")");
    }

    void not(Term term) {
        append("not(");
        toJava(term.getArg(0));
        append(")");
    }

    String getTermName(Term term) {
        switch (term.oper()) {
            case ExprType.EQ:
                return "eq";
            case ExprType.NEQ:
                return "neq";
            case ExprType.LE:
                return "le";
            case ExprType.LT:
                return "lt";
            case ExprType.GE:
                return "ge";
            case ExprType.GT:
                return "gt";

            case ExprType.PLUS:
                return "plus";
            case ExprType.MINUS:
                return "minus";
            case ExprType.MULT:
                return "mult";
            case ExprType.DIV:
                return "div";

            case ExprType.AND:
                return "&&";
            case ExprType.OR:
                return "||";


        }
        return "undef";
    }

    void functionCall(Term term) {
        switch (term.oper()) {
            case ExprType.IF:
                ifthenelse(term);
                return;

            case ExprType.RETURN:
                append("return");
                append(SPACE);
                toJava(term.getArg(0));
                return;

            case ExprType.HASH:
                hash(term);
                return;
                
            case ExprType.CAST:
                cast(term);
                return;
                
            case ExprType.SET:
                set(term);
                return;
                
            case ExprType.MAP:
            case ExprType.MAPLIST:
            case ExprType.MAPMERGE:
                map(term);
                return;
                
            case ExprType.APPLY_TEMPLATES_WITH:
            case ExprType.APPLY_TEMPLATES_WITH_ALL:
                template(term);
                return;
                
        }

        call(term);
    }
    
    String getExistVar() {
        return VAR_EXIST + count++;
    }
    
    
    /**
     * 
     * map(ex:fun(?x), ?list)
     */
    void map(Term term){
        if (term.oper() == ExprType.MAPMERGE){
            append(getFunctionName(Processor.XT_MERGE)).append("(");
        }
        
        append(mapName(term)).append("(");
        int i = 0;
        for (Expression exp : term.getArgs()) {
            toJava(exp);                
            if (i++ < term.arity() - 1) {
                append(", ");
            }
        }
        append(")");
        
        if (term.oper() == ExprType.MAPMERGE){
            append(")");
        }
    }
    
    String javaName(String name){
        return NSManager.nstrip(name);
    }

    String mapName(Term term){
        switch(term.oper()){
            case ExprType.MAP: return "map";
            case ExprType.MAPLIST: 
            case ExprType.MAPMERGE: return "maplist";
        }
        return "map";
    }
    
    void set(Term term) {
        toJava(term.getArg(0));
        append(" = ");
        toJava(term.getArg(1));
    }

        
    void cast(Term term){
        toJava(term.getArg(0));
        append(".cast(");
        toJava(term.getCName()); 
        append(")");
    }
    

    void hash(Term term) {
        append("hash(");
        append(dtc.string(term.getModality()));
        append(", ");
        toJava(term.getArg(0));
        append(")");
    }

    boolean isMethod(Term term) {
        String oper = term.getLabel();
        try {
            ClassLoader cl = getClass().getClassLoader();
            Class c = cl.loadClass(PROXY_PACKAGE);
            Class<IDatatype>[] aclasses = new Class[term.getArity()];
            for (int i = 0; i < aclasses.length; i++) {
                aclasses[i] = IDatatype.class;
            }
            c.getMethod(oper, aclasses);

            return true;

        } catch (ClassNotFoundException e) {
            logger.error(e);
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            logger.error(e);
        } catch (IllegalArgumentException e) {
        }
        return false;
    }

    void call(Term term) {
        if (isDatatypeMethod(term)) {
            method(term);
            return;
        }
        else if (term.getLongName().startsWith(NSManager.STL)){
            // st:get() -> pt.get()
            append("getPluginTransform().");
        }
        funcall(term);
    }
    
    boolean isDatatypeMethod(Term term){
        return getMethod(term) != null;
    }
    
    Method getMethod(Term term) {
        if (term.getArgs().isEmpty()){
            return null;
        }
        try {
            Class[] sig = new Class[term.getArgs().size()-1];
            Arrays.fill(sig, IDatatype.class);
            Method meth = IDatatype.class.getMethod(term.getName(), sig);
            return meth;
        } catch (NoSuchMethodException | SecurityException ex) {
        }
        return null;
    }
    
    boolean isBooleanMethod(Term term) {
        Method met = getMethod(term);
        return met != null && met.getReturnType() == boolean.class;
    }
    
    /*
     * st:apply-templates-with(trans, var)
     */
    void template(Term term){       
       append("getPluginTransform().transform(");
       append(dtc.newInstance(isAll(term)));
       for (Expression exp : term.getArgs()) {
           append(", ");
           toJava(exp);            
       }
       append(")");
    }
    
    boolean isAll(Term term){
        switch (term.oper()){
            case ExprType.APPLY_TEMPLATES_ALL:
            case ExprType.APPLY_TEMPLATES_WITH_ALL: return true;               
       } 
        return false;
    }
    
    void funcall(Term term){
        //append(term.javaName());
        append(getMethodName(term));
        append("(");
        int i = 0;
        for (Expression exp : term.getArgs()) {
            toJava(exp);
            if (i++ < term.arity() - 1) {
                append(", ");
            }
        }
        append(")");
    }
    
    void method(Term term) {
        toJava(term.getArg(0));
        append(".");
        append(getMethodName(term));
        append("(");
        int i = 0;
        for (Expression exp : term.getArgs()) {
            if (i > 0) {
                toJava(exp);
                if (i++ < term.arity() - 1) {
                    append(", ");
                }
            }
            else {
                i++;
            }
        }
        append(")");
    }
    
    String getMethodName(Term term) {
        String name = getFunctionName(term.getLabel());
        if (name == null) {
            return term.javaName();
        }
        return name;
    }
    
    String getFunctionName(String name) {
        return functionName.get(name);
    }

    void ifthenelse(Term term) {
        append("if (");
        toJava(term.getArg(0));
        append(".booleanValue()");
        append(") {");
        incrnl();
        toStatement(term.getArg(1));
        pv(term.getArg(1));
        decrnl();
        append("}");
        nl();
        append("else ");
        if (term.getArg(2).oper() == ExprType.IF) {
            ifthenelse(term.getArg(2).getTerm());
        } else {
            append("{");
            incrnl();
            toStatement(term.getArg(2));
            pv(term.getArg(2));
            decrnl();
            append("}");
        }
    }

    void toStatement(Expression exp) {
        if (exp.isConstant() || exp.isVariable()) {
            append("self(");
            toJava(exp);
            append(");");
        } else {
            toJava(exp);
        }
    }

    void sequence(Term term) {
        for (Expression exp : term.getArgs()) {
            toJava(exp);
            pv(exp);
            nl();
        }
    }
    
     /**
     * @return the function
     */
    public Function getFunction() {
        return function;
    }

    /**
     * @param function the function to set
     */
    public void setFunction(Function function) {
        this.function = function;
    }
}
