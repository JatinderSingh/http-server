/**
 * 
 */
package io.maelstorm.netty;

import java.lang.reflect.Method;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.bytecode.DuplicateMemberException;

/**
 * @author Jatinder
 *
 */
public final class AppendableCharSequenceAddon {
    private static Method subsequence;
    private static final ThreadLocal<Object[]> cachedVarargs = new ThreadLocal<>();
    static {
        try {
            Class clazz = null;
            CtClass ctclass = ClassPool.getDefault().get("io.netty.util.internal.AppendableCharSequence");
            try {
                CtMethod newmethod = CtNewMethod.make("public void subSequence(int start, int end, io.netty.util.internal.AppendableCharSequence seq) { if(seq.chars.length<end-start) { seq.chars = seq.expand(seq.chars, end-start, seq.chars.length); } seq.pos = end-start; System.arraycopy(chars, start, seq.chars, 0, seq.pos);}",ctclass);
                ctclass.addMethod(newmethod);
                ctclass.writeFile();
                clazz = ctclass.toClass();
            } catch (DuplicateMemberException dme) {
                dme.printStackTrace();
            }
            if (null==clazz) {
                clazz = io.netty.util.internal.AppendableCharSequence.class;
            }
            subsequence = clazz.getDeclaredMethod("subSequence", int.class, int.class, io.netty.util.internal.AppendableCharSequence.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AppendableCharSequence Injection/Initialization failed");
        }
    }
    
    public static void configure () {
        if (null==cachedVarargs.get()) {
            cachedVarargs.set(new Object[3]);
        }
    }
    
    public static void invokeSubsequence(io.netty.util.internal.AppendableCharSequence source, int start, int end, io.netty.util.internal.AppendableCharSequence sequence) {
        configure();
        try {
            Object[] varArgs = cachedVarargs.get();
            varArgs[0] = start;
            varArgs[1] = end;
            varArgs[2] = sequence;
            subsequence.invoke(source, varArgs);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            System.err.println(e);
        }
    }
}
