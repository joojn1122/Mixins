package com.joojn.mixins.element;

import com.joojn.mixins.util.MixinHelper;
import com.joojn.mixins.exception.MixinException;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;

import java.lang.reflect.Field;

public class NewField {

    public static FieldNode findField(
            ClassNode currentNode,
            Field field) throws MixinException
    {
        for(FieldNode node : currentNode.fields)
        {
            if(MixinHelper.fieldEquals(node, field))
            {
                return node;
            }
        }

        throw new MixinException("Something went went while trying to create new field %s", field.getName());
//            return new FieldNode(
//                    Modifier.isStatic(field.getModifiers()) ? Opcodes.GETSTATIC : Opcodes.GETFIELD,
//                    name,
//                    Type.getDescriptor(field.getType()),
//                    ReflectionHelper.getField(Field.class, "signature", field),
//                    value
//            );
    }
}
