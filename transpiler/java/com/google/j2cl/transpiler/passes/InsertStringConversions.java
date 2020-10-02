/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.transpiler.passes;

import com.google.j2cl.transpiler.ast.CompilationUnit;
import com.google.j2cl.transpiler.ast.Expression;
import com.google.j2cl.transpiler.ast.MethodCall;
import com.google.j2cl.transpiler.ast.MethodDescriptor;
import com.google.j2cl.transpiler.ast.PrimitiveTypes;
import com.google.j2cl.transpiler.ast.TypeDescriptor;
import com.google.j2cl.transpiler.ast.TypeDescriptors;

/**
 * Inserts a Strings.valueOf() operation when a non-string type is part of a "+" operation along
 * with the string type in a string conversion context.
 */
public class InsertStringConversions extends NormalizationPass {
  @Override
  public void applyTo(CompilationUnit compilationUnit) {
    compilationUnit.accept(new ConversionContextVisitor(getContextRewriter()));
  }

  private ConversionContextVisitor.ContextRewriter getContextRewriter() {
    return new ConversionContextVisitor.ContextRewriter() {
      @Override
      public Expression rewriteStringContext(Expression expression) {
        TypeDescriptor typeDescriptor = expression.getTypeDescriptor();
        if (expression.isNonNullString()) {
          return expression;
        }

        // Normally Java would call String.valueOf on a primitive but there is no need in J2CL
        // because JS converts primitives to String in the presence of a + operator.
        // We make an exception for Char which is represented as a JS number and hence needs to
        // be explicitly converted.
        // NOTE: Primitive longs are not represented as primitives in JS but are implemented as
        // instances of a class where we can rely on toString().
        if (TypeDescriptors.isNonVoidPrimitiveType(typeDescriptor)
            && !TypeDescriptors.isPrimitiveChar(typeDescriptor)) {
          return expression;
        }

        // For the normal case we call String.valueOf which performs the right conversion.
        return MethodCall.Builder.from(getStringValueOfMethodDescriptor(typeDescriptor))
            .setArguments(expression)
            .build();
      }
    };
  }

  private static MethodDescriptor getStringValueOfMethodDescriptor(TypeDescriptor typeDescriptor) {
    // Find the right overload.
    if (TypeDescriptors.isPrimitiveByte(typeDescriptor)
        || TypeDescriptors.isPrimitiveShort(typeDescriptor)) {
      typeDescriptor = PrimitiveTypes.INT;
    } else if (!typeDescriptor.isPrimitive()) {
      typeDescriptor = TypeDescriptors.get().javaLangObject;
    }

    return TypeDescriptors.get()
        .javaLangString
        .getMethodDescriptor(MethodDescriptor.VALUE_OF_METHOD_NAME, typeDescriptor);
  }
}
