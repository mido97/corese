package fr.inria.edelweiss.rif.ast;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import fr.inria.edelweiss.rif.api.RIF;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * This class is used to manage prefixes and namespaces.
 * <br>
 * @author Olivier Corby 
 */

public class NSManager 
{
	
	/** Use to keep the class version, to be consistent with the interface Serializable.java */
	private static final long serialVersionUID = 1L;
	
	/** logger from log4j */
	private static Logger logger = LogManager.getLogger(NSManager.class);
	
	/** prefix seed (ns1, ns2,...) */
	private static final String seed = "ns";
	
	public static final String NSsep="#";
	static final char[] end={'#', '/', '?', ':'}; // may end an URI ...
	static final String[] PROTOCOLS = 
	{ "http://", "file:", "ftp://", "mailto:", "urn:", "news:" };
	static final String pchar=":";
	int count=0;
	Hashtable<String, String> def; // system namespace with prefered prefix
	Hashtable<String, Integer> index;  // namespace -> number
	Hashtable<String, String> tns;     // namespace -> prefix
	Hashtable<String, String> tprefix; // prefix -> namespace
	String base = "";	// base
	
	private String uri, exp;
	private Object object;
	private Object dt;
	private boolean isValid = true, ishtDoc = true;
	private Hashtable <String, Object> htDoc;
	private Hashtable <String, Boolean> htValid;
	
	/** contains the prefix defined in the query */
	Vector<String> queryPrefix = new Vector<String>();
	
	/** 
	 * Corese.getDataLoader().get(CoreseProp.PGNS)
	 * Corresponds to the namespaces declared by default 
	 */
	private String defaultNamespaces = null;
	
	private NSManager(){
		def=new Hashtable<String, String>();
		index=new Hashtable<String, Integer>();
		tprefix=new Hashtable<String, String>();
		tns = new Hashtable<String, String>();
		define();
	}
	
	private NSManager(String defaultNamespaces) {
		this();
		this.defaultNamespaces = defaultNamespaces;
	}
	
	
	
	/**
	 * Warning: to be used only when we don't need default namespaces and their prefixes
	 * @return a NamespaceManager with defaultNamespaces = ""
	 */
	public static NSManager create() {
		NSManager nsm = new NSManager(null);
		nsm.init();
		return nsm;
	}
	
	
	public static NSManager create(String defaultNamespaces){
		NSManager nsm=new NSManager(defaultNamespaces);
		nsm.init();
		return nsm;
	}
	



	public void init(){
		initDefault();
		defNamespace();
	}
	
	public Enumeration getNamespaces(){
		return tns.keys();
	}
	
	public Enumeration getPrefixes(){
		return getPrefixEnum();
	}
	
	public void clear(){
		tns.clear();
		index.clear();
		tprefix.clear();
		count=0;
	}
	
	/**
	 * application specific namespace/prefix if any
	 */
	public void defNamespace(){
		if (defaultNamespaces != null) {
			String ns, p;
			StringTokenizer st=new StringTokenizer(defaultNamespaces);
			while (st.hasMoreTokens()){
				p=st.nextToken();
				ns=st.nextToken();
				defNamespace(ns, p);
			}
		}
	}
	
	// default system namespaces, not for application namespace
	 void define(){
		def.put(RIF.XML,  RIF.XMLPrefix);
		def.put(RIF.RDF,  RIF.RDFPrefix);
		def.put(RIF.RDFS, RIF.RDFSPrefix);
		def.put(RIF.XSD,  RIF.XSDPrefix);
		def.put(RIF.OWL,  RIF.OWLPrefix);
	}
	
	// add default namespaces
	 void initDefault(){
		Enumeration<String> en=def.keys();
		String ns;
		while (en.hasMoreElements()){
			ns=en.nextElement();
			defNamespace(ns, def.get(ns));
		}
	}
	
	
	public boolean isSystem(String ns){
		return def.containsKey(ns);
	}
	
	public boolean isNamespace(String ns){
		return tns.containsKey(ns);
	}
	
	/** Define a namespace, returns the prefix
	 */
	public String defNamespace(String ns){
		if (ns==null)
			return null;
		else
			if (! tns.containsKey(ns)){
				defNamespace(ns, makePrefix(ns));
			}
		return getPrefix(ns);
	}
	
	
	String makePrefix(String ns){
		if (def.get(ns)!=null)
			return def.get(ns);
		else {
			return createPrefix(seed);
		}
	}
	
	String createPrefix(String p){
		if (! p.equals(seed))
			return p;
		String str= (count==0)?seed:(seed+ count);
		count++;
		return str;
	}
	
	public String defNamespace(String ns, String prefix){
		if (ns!=null && prefix!=null){
			prefix=createPrefix(prefix);
			if (! tns.containsKey(ns))
				tns.put(ns, prefix);
			defPrefix(prefix, ns);
			index.put(ns, new Integer(tns.size()));
		}
		return prefix;
	}
	
	public String defNamespaceQuery(String ns, String prefix){
		if (!(queryPrefix.contains(prefix)))
			queryPrefix.add(prefix);
		return defNamespace(ns, prefix);
	}
	
	public Vector<String> getQueryPrefix() {
		return queryPrefix;
	}
	
	/** Returns the prefix of the namespace
	 */
	public String getPrefix(String ns) {
		return (String) tns.get(ns);
	}
	
	void defPrefix(String prefix, String ns) {
		tprefix.put(prefix, ns);
	}
	
	public String getNamespace(String prefix) {
		return (String)tprefix.get(prefix);
	}
	
	Enumeration getPrefixEnum(){
		return tprefix.keys();
	}
	
	public int getIndex(String ns){
		if (getPrefix(ns)==null)
			defNamespace(ns);
		return (index.get(ns)).intValue();
	}
	
	public String toPrefix(String nsname){
		return toPrefix(nsname, false);
	}
	
	/**
	 * If skip, if no prefix for this namespace, return nsname,
	 * else create a prefix
	 */
	public String toPrefix(String nsname, boolean skip){
		String ns=namespace(nsname);
		if (ns==null || ns.equals(""))
			return nsname;
		String p=getPrefix(ns);
		if (p==null){
			if (skip)
				return nsname;
			else p=defNamespace(ns);
		}
		String str=p + pchar + nsname.substring(ns.length());
		return str;
	}
	
	
	
	/**
	 * pname is a QNAME, expand with namespace
	 * @param pname
	 * @return
	 */
	public String toNamespace(String pname){
		if (pname == null) return null;
		Enumeration en=getPrefixEnum();
		String p;
		while (en.hasMoreElements()){
			p=(String)en.nextElement();
			if (pname.startsWith(p) && pname.indexOf(pchar)==p.length()){
				return getNamespace(p) + pname.substring(p.length()+1);
			}
		}
		return pname;
	}
	
	/**
	 * same as function 'toNamespace' + pname may be a 'base' (ex: comes from
	 * <Person>)
	 * 
	 * @param pname
	 * @return
	 */
	public String toNamespaceB(String pname){
		pname = toNamespace(pname);
		if (isBase() && ! isURN(pname) && !isVariable(pname)) {
			String toReturn = getBase() + pname;
			return toReturn;
		} 
		return pname;
	}
	
	boolean isVariable(String pname){
		return pname.startsWith("?");
	}
	
	boolean isURN(String str) {
		for (int i = 0; i < PROTOCOLS.length; i++) {
			if (str.startsWith(PROTOCOLS[i]))
				return true;
		}
		return false;
	}
	
	public void setBase(String s) {
		base = s;
	}
	
	public boolean isBase() {
		return (base != null);
	}
	
	public String getBase() {
		return base;
	}
	
	/**
	 * Return the namespace of this QName
	 */
	public String getQNamespace(String pname) {
		Enumeration en = getPrefixEnum();
		String p;
		while (en.hasMoreElements()) {
			p = (String) en.nextElement();
			if (pname.startsWith(p) && pname.indexOf(pchar) == p.length()) {
				return getNamespace(p);
			}
		}
		return null;
	}
	
	public String getPackage(String qname){
		String ns = getQNamespace(qname);
		return ns;
		
	}
	
	
	// from  external to internal id (from  cosbn#genid1 to _bn#genid1)
//	used in query language to retrieve a BN from its external id
//	public static String getInternalBlankNodeID(String id) {
//		String str = NSManager.nstrip(id); // remove cosbn, str=genid1
//		return NSParser.getInternalID(str); // return  _bn#genid1
//	}
	
	public static boolean isBlankNodeID(String id){
		return false;
	}
	
	/**
	 * toNamespace , if it is an external BN uri, return the internal BN uri
	 */
	public String toNamespaceBN(String str) {
		return toNamespaceB(str);
		
//		if (isBlankNodeID(str)) { // query external cosbn#genid1
//			return getInternalBlankNodeID(str); // query internal _bn#genid1
//		}
//		else {
//			str = toNamespaceB(str);
//			if (isBlankNodeID(str)) // query external cosbn#genid1
//				return getInternalBlankNodeID(str); // query internal _bn#genid1
//			else
//				return str;
//		}
	}
	
	
	public int size(){
		return tns.size();
	}
	
	public String decNamespace(){
		Enumeration en=tns.keys();
		String ns;
		String dec=new String("");
		while (en.hasMoreElements()){
			ns=(String)en.nextElement();
			dec+="xmlns:"+getPrefix(ns) + "='" + ns + "'";
			if (en.hasMoreElements()) dec+=" ";
		}
		return dec;
	}
	
	public String stripns(String name, String namespace, boolean refp){
		// if namespace not null, removes it
		// if refp add a #
		return ((namespace!=null) && (inNamespace(name, namespace)))?
				((refp)?NSsep+strip(name):strip(name)):name;
	}
	
	public String strip(String name){
		// remove namespace and #
		return  nstrip(name);
	}
	
//	public String blankID(String name){
//		return NSParser.blankID(name);
//	}
	
	public String strip(String name, String ns){
		// remove ns from name
		return  name.substring(ns.length());
	}
	
	
	public static String nstrip(String name){
		// remove namespace
		int index;
		for (int i=0; i<end.length; i++){
			index=name.lastIndexOf(end[i]);// ???
			if (index!=-1)
				return  name.substring(index+1);
		}
		return name;
	}
	
	public boolean sysNamespace(String name){
		Enumeration en=def.keys();
		String ns;
		while (en.hasMoreElements()){
			ns=(String)en.nextElement();
			if (inNamespace(name, ns))
				return true;
		}
		return false;
	}
	
	public boolean inNamespace(String type, String namespace)
	// retourne si un type appartient au namespace
	{
		if (namespace==null) return true;
		else return type.startsWith(namespace);
	};
	
	
	public  static String namespace(String type){  //retourne le namespace d'un type
		if (type.startsWith(NSsep))
			return "";
		int index;
		for (int i=0; i<end.length; i++){
			index=type.lastIndexOf(end[i]);
			if (index!=-1){
				String str= type.substring(0, index+1);
				return str;
			}
		}
		return "";
	}
	
	/*
	 return last occurrence of pat (e.g. '/') in str
	 if pat is last, find preceding occurrence
	 */
	static int getIndex(String str, char pat){
		int index=str.lastIndexOf(pat);
		if (index == str.length()-1){
			logger.debug(str + " " + index + " " + str.lastIndexOf(pat, index));
			return str.lastIndexOf(pat, index-1);
		}
		else return index;
	}
	
	public static String putNamespace(String ns, String label){
		return ns+label;
	}

	public String getDefaultNamespaces() {
		return defaultNamespaces;
	}
	
	// xpath document:
	public void set(String name, Object obj){
		uri = name;
		object = obj;
		isValid = obj != null;
		if (ishtDoc){
			htValid.put(name, (isValid) ? Boolean.TRUE : Boolean.FALSE);
			if (isValid) htDoc.put(name, obj);
		}
	}
	
	public Object get(String name){
		if (uri != null && name.equals(uri)){
			return object;
		}
		else if (ishtDoc){
			uri = name;
			exp = null;
			object = htDoc.get(name);
			return object;
		}
		else return null;
	}
	
	public boolean isValid(String name){
		if (ishtDoc){
			if (htDoc == null){
				htDoc = new Hashtable<String,Object>();
				htValid = new Hashtable<String,Boolean>();
			}
			Boolean valid = htValid.get(name);
			return (valid == null || valid);
		}
		if (uri != null && name.equals(uri)){
			return isValid;
		}
		else return true;
	}
	
	// xpath expression:
	public void put(String name, String ee, Object val){
		uri = name;
		exp = ee;
		dt = val;
	}
	
	public Object pop(String name, String ee){
		if (exp != null && exp.equals(ee) && uri != null && uri.equals(name)){
			return dt;
		}
		else return null;
	}
	
	
	
	
	
	
	
	

}
