/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999-2003 Shigeru Chiba. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package javassist.convert;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.*;
import javassist.CannotCompileException;

public class TransformCall extends Transformer {
    protected String classname, methodname, methodDescriptor;
    protected String newClassname, newMethodname;

    /* cache */
    protected int newIndex;
    protected ConstPool constPool;

    public TransformCall(Transformer next, CtMethod origMethod,
                         CtMethod substMethod)
    {
        super(next);
        this.classname = origMethod.getDeclaringClass().getName();
        this.methodname = origMethod.getName();
        this.methodDescriptor = origMethod.getMethodInfo2().getDescriptor();
        this.newClassname = substMethod.getDeclaringClass().getName();
        this.newMethodname = substMethod.getName();
        this.constPool = null;
    }

    public void initialize(ConstPool cp, CodeAttribute attr) {
        if (constPool != cp)
            newIndex = 0;
    }

    /**
     * Modify INVOKEINTERFACE, INVOKESPECIAL, INVOKESTATIC and INVOKEVIRTUAL
     * so that a different method is invoked.
     */
    public int transform(CtClass clazz, int pos, CodeIterator iterator,
                         ConstPool cp) throws BadBytecode
    {
        int c = iterator.byteAt(pos);
        if (c == INVOKEINTERFACE || c == INVOKESPECIAL
                        || c == INVOKESTATIC || c == INVOKEVIRTUAL) {
            int index = iterator.u16bitAt(pos + 1);
            int typedesc = cp.isMember(classname, methodname, index);
            if (typedesc != 0)
                if (cp.getUtf8Info(typedesc).equals(methodDescriptor))
                    pos = match(c, pos, iterator, typedesc, cp);
        }

        return pos;
    }

    protected int match(int c, int pos, CodeIterator iterator,
                        int typedesc, ConstPool cp) throws BadBytecode
    {
        if (newIndex == 0) {
            int nt = cp.addNameAndTypeInfo(cp.addUtf8Info(newMethodname),
                                           typedesc);
            int ci = cp.addClassInfo(newClassname);
            if (c == INVOKEINTERFACE)
                newIndex = cp.addInterfaceMethodrefInfo(ci, nt);
            else
                newIndex = cp.addMethodrefInfo(ci, nt);

            constPool = cp;
        }

        iterator.write16bit(newIndex, pos + 1);
        return pos;
    }
}
