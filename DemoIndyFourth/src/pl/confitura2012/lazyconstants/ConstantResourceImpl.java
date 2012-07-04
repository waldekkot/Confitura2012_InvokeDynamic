package pl.confitura2012.lazyconstants;

public class ConstantResourceImpl implements ConstantResource {
	public static final String xml;
	static {
		System.out.println("Called in the class initializer (<CLINIT>).");
		xml = ParseHelper.doSomeHeavyParsing("test.xml");		
	}
	
	@Override
	public void notNeedingTheXMLHere() {  
		//But, I am NOT using the 'xml' constant here !
		System.out.println("Called 'notNeedingTheXMLHere'");
	}

	@Override
	public void badlyNeedingTheXMLHere() {  
		//I will be using the 'xml' constant here !
		System.out.println("Called 'notNeedingTheXMLHere'");
		try {
			System.out.println(xml);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
