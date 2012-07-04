package pl.confitura2012.lazyconstants;

public class LazyConstantsMainWithIndy {

	public static void main(String[] args) {
		ConstantResource testObject = new ConstantResourceImplWithIndy("test.xml");
		//testObject.notNeedingTheXMLHere();
		testObject.badlyNeedingTheXMLHere();
	}
}
