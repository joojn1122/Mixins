package net.mixins;

import com.joojn.mixins.annotation.MixinMethod;
import com.joojn.mixins.annotation.MixinTarget;
import com.joojn.mixins.annotation.Node;
import com.joojn.mixins.annotation.Shadow;
import org.objectweb.asm.Opcodes;

@MixinTarget(className="Test")
public abstract class MinecraftMixin {

    @Shadow(
            node = @Node(
                    name = "toString",
                    owner = "java.lang.Object",
                    opcode = Opcodes.INVOKESPECIAL
            )
    )
    public abstract String toString_();

    @MixinMethod(override = true)
    public void test()
    {
        System.out.println(toString_());
        System.out.println(this);
    }
}
