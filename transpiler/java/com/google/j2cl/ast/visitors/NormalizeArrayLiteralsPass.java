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
package com.google.j2cl.ast.visitors;

import com.google.j2cl.ast.AbstractRewriter;
import com.google.j2cl.ast.AbstractVisitor;
import com.google.j2cl.ast.ArrayLiteral;
import com.google.j2cl.ast.ArrayTypeDescriptor;
import com.google.j2cl.ast.CompilationUnit;
import com.google.j2cl.ast.Expression;
import com.google.j2cl.ast.NewArray;
import com.google.j2cl.ast.Node;
import com.google.j2cl.ast.NullLiteral;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Rewrites the rare short form array literal initializers (like "int[] foo = {1, 2, 3};") into the
 * more common long form (like "int[] foo = new int[] {1, 2, 3};").
 */
public class NormalizeArrayLiteralsPass {

  private Set<ArrayLiteral> longFormArrayLiterals = new HashSet<>();

  public static void applyTo(CompilationUnit compilationUnit) {
    new NormalizeArrayLiteralsPass().normalizeArrayLiterals(compilationUnit);
  }

  private void normalizeArrayLiterals(CompilationUnit compilationUnit) {
    compilationUnit.accept(new CollectLongFormArrayLiteralsVisitor());
    compilationUnit.accept(new RewriteShortFormToLongFormRewriter());
  }

  private class CollectLongFormArrayLiteralsVisitor extends AbstractVisitor {
    @Override
    public boolean enterNewArray(NewArray newArray) {
      if (newArray.getArrayLiteral() != null) {
        longFormArrayLiterals.add(newArray.getArrayLiteral());
      }
      return true;
    }
  }

  private class RewriteShortFormToLongFormRewriter extends AbstractRewriter {
    @Override
    public Node rewriteArrayLiteral(ArrayLiteral arrayLiteral) {
      if (longFormArrayLiterals.contains(arrayLiteral)) {
        return arrayLiteral;
      }

      // Rewrite ArrayLiteral as NewArray(ArrayLiteral).
      ArrayTypeDescriptor arrayTypeDescriptor = arrayLiteral.getTypeDescriptor();
      return new NewArray(
          arrayTypeDescriptor,
          Collections.<Expression>nCopies(arrayTypeDescriptor.getDimensions(), NullLiteral.NULL),
          arrayLiteral);
    }
  }
}
