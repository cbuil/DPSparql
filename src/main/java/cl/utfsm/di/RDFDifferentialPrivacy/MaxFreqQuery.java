package cl.utfsm.di.RDFDifferentialPrivacy;

public class MaxFreqQuery {
    private StarQuery query;
    private String variable;

    public MaxFreqQuery(StarQuery tp, String var) {
        query = tp;
        variable = var;
    }

    public int getQuerySize() {
        return query.getTriples().size();
    }

    public StarQuery getQuery() {
        return query;
    }

    public String getVariableString() {
        return variable;
    }
    
    @Override
    public String toString(){
        return query.getTriples().toString();
    }
            
}
