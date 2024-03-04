import org.junit.internal.TextListener
import org.junit.runner.JUnitCore

def junit = new JUnitCore()
junit.addListener(new TextListener(System.out))
junit.run(
    MessageTests.class,
    UtilsTests.class,
    EndToEndTests.class
)