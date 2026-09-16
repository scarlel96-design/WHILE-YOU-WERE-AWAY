import java.lang.instrument.*;
import java.security.ProtectionDomain;
import jdk.internal.org.objectweb.asm.*;
/** Development launcher only. Place this JVM's GLFW windows before they first become visible. */
public final class MonitorAgent {
    public static void premain(String args,Instrumentation inst) {
        String[] xy=args.split(",");final int x=Integer.parseInt(xy[0]),y=Integer.parseInt(xy[1]);
        inst.addTransformer(new ClassFileTransformer(){
            public byte[] transform(ClassLoader loader,String name,Class<?> clazz,ProtectionDomain domain,byte[] bytes) {
                if(!name.equals("org/lwjgl/glfw/GLFW"))return null;
                ClassReader reader=new ClassReader(bytes);ClassWriter writer=new ClassWriter(reader,ClassWriter.COMPUTE_MAXS);
                reader.accept(new ClassVisitor(Opcodes.ASM8,writer){
                    public MethodVisitor visitMethod(int access,String method,String desc,String signature,String[] exceptions) {
                        MethodVisitor out=super.visitMethod(access,method,desc,signature,exceptions);
                        if((access&Opcodes.ACC_NATIVE)!=0)return out;
                        return new MethodVisitor(Opcodes.ASM8,out){
                            public void visitCode(){super.visitCode();
                                if(method.equals("glfwCreateWindow")) {
                                    visitLdcInsn(131086);visitLdcInsn(x);visitMethodInsn(Opcodes.INVOKESTATIC,name,"glfwWindowHint","(II)V",false);
                                    visitLdcInsn(131087);visitLdcInsn(y);visitMethodInsn(Opcodes.INVOKESTATIC,name,"glfwWindowHint","(II)V",false);
                                }
                                if(method.equals("glfwSetWindowPos")) {visitLdcInsn(x);visitVarInsn(Opcodes.ISTORE,2);visitLdcInsn(y);visitVarInsn(Opcodes.ISTORE,3);}
                                if(method.equals("glfwSetWindowMonitor")) {visitLdcInsn(x);visitVarInsn(Opcodes.ISTORE,4);visitLdcInsn(y);visitVarInsn(Opcodes.ISTORE,5);}
                            }
                        };
                    }
                },0);
                System.out.println("MONITOR_AGENT configured GLFW creation/placement x="+x+" y="+y);
                return writer.toByteArray();
            }
        });
    }
}
