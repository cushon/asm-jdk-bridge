package codes.rafael.asmjdkbridge.test;

import codes.rafael.asmjdkbridge.JdkClassReader;
import codes.rafael.asmjdkbridge.JdkClassWriter;
import org.junit.Test;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.io.InputStream;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class AsmAttributeTest {

    @Test
    public void can_read_attribute_properties() throws Exception {
        Class<?> target = CustomAttributeExtractable.make();
        byte[] classFile;
        try (InputStream inputStream = target.getResourceAsStream(target.getName().substring(target.getPackageName().length() + 1) + ".class")) {
            classFile = inputStream.readAllBytes();
        }
        StringBuilder asm = new StringBuilder(), jdk = new StringBuilder();
        new ClassReader(classFile).accept(new ClassVisitor(Opcodes.ASM9) {
        }, new Attribute[] {new StringCollectingAttribute(asm)}, 0);
        new JdkClassReader(classFile, new StringCollectingAttribute(jdk)).accept(new ClassVisitor(Opcodes.ASM9) {
        }, 0);
        assertEquals(asm.toString(), jdk.toString());
    }

    @Test
    public void can_write_attribute() {
        StringBuilder asm = new StringBuilder(), jdk = new StringBuilder();
        apply(new ClassWriter(0), asm, ClassWriter::toByteArray);
        apply(new JdkClassWriter(0), jdk, ignored -> { });
        assertEquals(asm.toString(), jdk.toString());
    }

    static <T extends ClassVisitor> void apply(T classVisitor, StringBuilder sb, Consumer<T> completion) {
        classVisitor.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/Attributes", null, "java/lang/Object", null);
        classVisitor.visitAttribute(new StringCollectingAttribute(sb));
        classVisitor.visitEnd();
        completion.accept(classVisitor);
    }

    static class StringCollectingAttribute extends Attribute {

        private final StringBuilder sb;

        private boolean written;

        StringCollectingAttribute(StringBuilder sb) {
            super("CustomAttribute");
            this.sb = sb;
        }

        @Override
        protected Attribute read(ClassReader classReader, int offset, int length, char[] charBuffer, int codeAttributeOffset, Label[] labels) {
            sb.append("----").append("\n");
            sb.append(offset).append("\n");
            sb.append(length).append("\n");
            sb.append(codeAttributeOffset).append("\n");
            sb.append(labels == null).append("\n");
            sb.append(classReader.getClassName()).append("\n");
            sb.append(classReader.getSuperName()).append("\n");
            sb.append(classReader.readByte(10)).append("\n");
            sb.append(classReader.readShort(10)).append("\n");
            sb.append(classReader.readUnsignedShort(10)).append("\n");
            sb.append(classReader.readInt(10)).append("\n");
            sb.append(classReader.readLong(10)).append("\n");
            sb.append(classReader.getItemCount()).append("\n");
            sb.append(classReader.readConst(2, charBuffer)).append("\n");
            sb.append(classReader.readClass(137, charBuffer)).append("\n");
            sb.append(classReader.readUTF8(175, charBuffer)).append("\n");
            return super.read(classReader, offset, length, charBuffer, codeAttributeOffset, labels);
        }

        @Override
        protected ByteVector write(ClassWriter classWriter, byte[] code, int codeLength, int maxStack, int maxLocals) {
            if (!written) {
                sb.append("----").append("\n");
                sb.append(classWriter.newConst("const")).append("\n");
                sb.append(classWriter.newUTF8("utf8")).append("\n");
                sb.append(classWriter.newField("owner", "name", "Ljava/lang/Object;")).append("\n");
                sb.append(classWriter.newMethod("owner", "name", "()V", false)).append("\n");
                sb.append(classWriter.newMethodType("()V")).append("\n");
                sb.append(classWriter.newNameType("name", "Ljava/lang/Object;")).append("\n");
                sb.append(classWriter.newModule("module")).append("\n");
                sb.append(classWriter.newClass("class")).append("\n");
                sb.append(classWriter.newPackage("package")).append("\n");
                sb.append(classWriter.newHandle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()V", false)).append("\n");
                sb.append(classWriter.newInvokeDynamic("name",
                        "()V",
                        new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()V", false),
                        "bootstrap")).append("\n");
                sb.append(classWriter.newConstantDynamic("name",
                        "Ljava/lang/Object;",
                        new Handle(Opcodes.H_INVOKEVIRTUAL, "owner", "name", "()V", false),
                        "bootstrap")).append("\n");
                written = true;
            }
            ByteVector vector = new ByteVector(1);
            vector.putByte(1);
            return vector;
        }
    }
}
