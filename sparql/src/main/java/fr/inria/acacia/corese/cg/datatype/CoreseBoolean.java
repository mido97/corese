package fr.inria.acacia.corese.cg.datatype;

import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.exceptions.CoreseDatatypeException;
import fr.inria.edelweiss.kgram.api.core.Loopable;

/**
 * <p>Title: Corese</p>
 * <p>Description: A Semantic Search Engine</p>
 * <p>Copyright: Copyright INRIA (c) 2007</p>
 * <p>Company: INRIA</p>
 * <p>Project: Acacia</p>
 * <br>
 * An implementation of the xsd:boolean datatype used by Corese
 * <br>
 * @author Olivier Corby & Olivier Savoie
 */

public class CoreseBoolean extends CoreseStringLiteral {
  static int  code=BOOLEAN;
  static String STRUE="true";
  static String SFALSE="false";
  public static final CoreseBoolean TRUE =  new CoreseBoolean(true);
  public static final CoreseBoolean FALSE = new CoreseBoolean(false);
  static final CoreseURI datatype=new CoreseURI(RDF.xsdboolean);
  boolean bvalue=true;
  private Object object;

  /**
   * Construct a Corese boolean
   * @param value <code>true</code> or <code>false</code>
   */
  public CoreseBoolean(String value) throws CoreseDatatypeException {
      super(getNormalizedLabel(value));
      if (!parse(value)){
        throw new CoreseDatatypeException("Boolean", value);
      }
  }
  
  public CoreseBoolean(boolean b)  {
      super((b)?STRUE:SFALSE);
      bvalue=b;
  }
  
  boolean parse(String value){
	  if (value.equals(SFALSE) || value.equals("0")){
    	  bvalue=false;
    	  return true;
      }
      else if (value.equals(STRUE) || value.equals("1")){
          bvalue=true;
          return true;
      }
	  return false;
  }

  @Override
  public boolean isTrue() {
     return bvalue;
   }
  
  @Override
  public boolean booleanValue(){
      return bvalue;
  }

  @Override
   public boolean isTrueAble() {
     return true;
   }
   
   int value(){
	   return (bvalue)?1:0;
   }

   /**
    * Cast a boolean to integer return 0/1
    */
  @Override
   public IDatatype cast(IDatatype target, IDatatype javaType) {
	   String lab = target.getLabel();
	   if (lab.equals(RDF.xsdinteger)) {
		   return DatatypeMap.newInstance(value());
	   } else if (lab.equals(RDF.xsdfloat)) {
		   return new CoreseFloat(value());
	   } else if (lab.equals(RDF.xsddouble)) {
		   return new CoreseDouble(value());
	   } else if (lab.equals(RDF.xsddecimal)) {
		   return new CoreseDecimal(value());
	   }
	   else return super.cast(target, javaType);
   }


  @Override
  public  int getCode(){
     return code;
   }

  @Override
   public IDatatype getDatatype(){
        return datatype;
      }
  
    @Override
    public boolean isBoolean() {
        return true;
    }


  /**
   * Normalized following the W3C xsd specification the label
   * @param label may be <code>true</code>, <code>false</code> or <code>1</code> or <code>0</code>
   * @return <code>true</code> or <code>false</code>
   */
     public static String getNormalizedLabel(String label){
        if (label.equals(STRUE) || label.equals("1") || label.equals(SFALSE) || label.equals("0")){
          return label;
        }
        else {
            return null;
        }
    }
     
  public static String getNormalizedLabel2(String label){
        if (label.equals(STRUE) || label.equals("1"))
          return STRUE;
        else if (label.equals(SFALSE) || label.equals("0"))
          return SFALSE;
        else return null;
    }

  @Override
  public boolean equalsWE(IDatatype iod) throws CoreseDatatypeException {
	  switch (iod.getCode()){
	  case BOOLEAN: return  booleanValue() == iod.booleanValue();// getLabel().equals(iod.getLabel());
	  case URI:
	  case BLANK: return false;
	  }
	  throw failure();
  }
  
  @Override
  public boolean less(IDatatype dt) throws CoreseDatatypeException {
      switch (dt.getCode()){
          case BOOLEAN: return ! booleanValue() && dt.booleanValue();	  
      }
      throw failure();              
  }
  
  @Override
  public boolean lessOrEqual(IDatatype dt) throws CoreseDatatypeException {
      switch (dt.getCode()){
          case BOOLEAN: return (booleanValue() == dt.booleanValue()) || (! booleanValue() && dt.booleanValue());	  
      }
      throw failure();              
  }
   
  @Override
  public boolean greaterOrEqual(IDatatype dt) throws CoreseDatatypeException {
      switch (dt.getCode()){
          case BOOLEAN: return (booleanValue() == dt.booleanValue()) || (booleanValue() && ! dt.booleanValue());	  
      }
      throw failure();              
  }
     
  @Override
  public boolean greater(IDatatype dt) throws CoreseDatatypeException {
      switch (dt.getCode()){
          case BOOLEAN: return booleanValue() && ! dt.booleanValue();	  
      }
      throw failure();              
  } 
  
  @Override
  public int compare(IDatatype dt) throws CoreseDatatypeException{
      switch (dt.getCode()){
          case BOOLEAN:
              if (booleanValue() == dt.booleanValue()){
                  return 0;
              }
              else if (booleanValue()){
                  return 1;
              }
              else {
                  return -1;
              }
      }
      throw failure();
  }
     

    /**
     * @return the object
     */
  @Override
    public Object getObject() {
        return object;
    }

    /**
     * @param object the object to set
     */
  @Override
    public void setObject(Object object) {
        this.object = object;
    }
  
  @Override
   public boolean isLoop(){
        return object != null && object instanceof Loopable;
    }
    
  @Override
    public Iterable getLoop(){
        return ((Loopable) object).getLoop();
    }
  

}
