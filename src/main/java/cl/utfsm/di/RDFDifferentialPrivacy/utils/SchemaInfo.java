package cl.utfsm.di.RDFDifferentialPrivacy.utils;

/**
 * SchemaInfo
 */
public class SchemaInfo {

	public int mSize;

    public String mSchemaName;

    public String mEndpoint;

    public SchemaInfo(int size, String schemaName, String endpoint){
        mSize = size;
        mEndpoint = endpoint;
        mSchemaName = schemaName;
    }
}
