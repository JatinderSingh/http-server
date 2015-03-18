/**
 * 
 */
package singh.jatinder.netty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;

/**
 * @author Jatinder
 *
 */
public final class AppendableCharSequenceAddon {
    private static Method subsequence;
    static {
        try {
            CtClass ctclass = ClassPool.getDefault().get("io.netty.util.internal.AppendableCharSequence");
            CtMethod newmethod = CtNewMethod.make("public void subSequence(int start, int end, io.netty.util.internal.AppendableCharSequence seq) { if(seq.chars.length<end-start) { seq.chars = seq.expand(seq.chars, end-start, seq.chars.length); } seq.pos = end-start; System.arraycopy(chars, start, seq.chars, 0, seq.pos);}",ctclass);
            ctclass.addMethod(newmethod);
            ctclass.writeFile();

            /*for(Method me: ctclass.toClass().getDeclaredMethods()){ //test print, ok
                System.out.print(me.getName() + "\t");
                for (Class clazz :me.getParameterTypes()) {
                    System.out.print(clazz.getCanonicalName()+ "\t");
                }
                System.out.println();
            }*/
            subsequence = ctclass.toClass().getDeclaredMethod("subSequence", int.class, int.class, io.netty.util.internal.AppendableCharSequence.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void configure () {
        
    }
    
    public static void invokeSubsequence(io.netty.util.internal.AppendableCharSequence source, int start, int end, io.netty.util.internal.AppendableCharSequence sequence) {
        try {
            subsequence.invoke(source, start, end, sequence);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.err.println(e);
        }
    }
}
