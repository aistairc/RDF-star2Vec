package jp.go.aist.airc.dkirt.rdfstar2vec.walk_generation.data_structures;

/**
 * Data structure for a triple whereby the object can be a string or a URI.
 */
public class QuotedTriple extends Triple {


    /**
     * Constructor
     * @param qt Quoted Triple node.
     * @param subject Subject
     * @param predicate Predicate
     * @param object Object
     */
    public QuotedTriple(String qt, String subject, String predicate, String object){
    	super(subject, predicate, object);
    	this.qt = qt;
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }
    
    public String qt;
    public String subject;
    public String predicate;
    public String object;
    

    @Override
    public boolean equals(Object obj){
        if (this == obj) return true;
        if (!(obj instanceof QuotedTriple)) return false;
        QuotedTriple that = (QuotedTriple) obj;
        return this.subject.equals(that.subject) && this.predicate.equals(that.predicate) && this.object.equals(that.object);
    }

    @Override
    public int hashCode(){
        return (subject + "_1").hashCode() + (predicate + "_2").hashCode() + (object + "_2").hashCode();
    }
}
