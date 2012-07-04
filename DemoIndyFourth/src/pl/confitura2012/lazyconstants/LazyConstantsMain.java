package pl.confitura2012.lazyconstants;

public class LazyConstantsMain {

	public static void main(String[] args) {
		ConstantResource testObject = new ConstantResourceImpl();
		testObject.notNeedingTheXMLHere();				
	}
}
