/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jp.co.yahoo.yosegi.inmemory;

import java.io.IOException;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.memory.BufferAllocator;
import org.apache.arrow.vector.SchemaChangeCallBack;
import org.apache.arrow.vector.complex.StructVector;
import org.apache.arrow.vector.complex.reader.*;
import org.apache.arrow.vector.complex.reader.BaseReader.*;
import org.apache.arrow.vector.complex.impl.*;
import org.apache.arrow.vector.complex.writer.*;
import org.apache.arrow.vector.complex.writer.BaseWriter.*;
import org.apache.arrow.vector.types.pojo.*;
import org.apache.arrow.vector.types.pojo.ArrowType.Struct;

import jp.co.yahoo.yosegi.message.objects.*;

import jp.co.yahoo.yosegi.spread.column.*;
import jp.co.yahoo.yosegi.binary.*;
import jp.co.yahoo.yosegi.binary.maker.*;

public class TestArrowDoubleMemoryAllocator{

  @Test
  public void T_setDouble_1() throws IOException{
    BufferAllocator allocator = new RootAllocator( 1024 * 1024 * 10 );
    SchemaChangeCallBack callBack = new SchemaChangeCallBack();
    StructVector parent = new StructVector("root", allocator, new FieldType(false, Struct.INSTANCE, null, null), callBack);
    parent.allocateNew();
    IMemoryAllocator memoryAllocator = ArrowMemoryAllocatorFactory.getFromStructVector( ColumnType.DOUBLE , "target" , allocator , parent , 4 );

    memoryAllocator.setDouble( 0 , (double)0.1 );
    memoryAllocator.setDouble( 1 , (double)0.2 );
    memoryAllocator.setDouble( 5 , (double)0.255 );
    memoryAllocator.setDouble( 1000 , (double)0.1 );

    StructReader rootReader = parent.getReader();
    FieldReader reader = rootReader.reader( "target" );
    reader.setPosition( 0 );
    assertEquals( reader.readDouble().doubleValue() , (double)0,1 );
    reader.setPosition( 1 );
    assertEquals( reader.readDouble().doubleValue() , (double)0.2 );
    reader.setPosition( 5 );
    assertEquals( reader.readDouble().doubleValue() , (double)0.255 );
    for( int i = 6 ; i < 1000 ; i++ ){
      reader.setPosition( i );
      assertEquals( reader.readDouble() , null );
    }
    reader.setPosition( 1000 );
    assertEquals( reader.readDouble().doubleValue() , (double)0.1 );
  }

  @Test
  public void T_setDouble_2() throws IOException{
    IColumn column = new PrimitiveColumn( ColumnType.DOUBLE , "boolean" );
    column.add( ColumnType.DOUBLE , new DoubleObj( (double)100 ) , 0 );
    column.add( ColumnType.DOUBLE , new DoubleObj( (double)200 ) , 1 );
    column.add( ColumnType.DOUBLE , new DoubleObj( (double)255 ) , 5 );

    ColumnBinaryMakerConfig defaultConfig = new ColumnBinaryMakerConfig();
    ColumnBinaryMakerCustomConfigNode configNode = new ColumnBinaryMakerCustomConfigNode( "root" , defaultConfig );

    IColumnBinaryMaker maker = new UnsafeOptimizeDoubleColumnBinaryMaker();
    ColumnBinary columnBinary = maker.toBinary( defaultConfig , null , column );

    BufferAllocator allocator = new RootAllocator( 1024 * 1024 * 10 );
    SchemaChangeCallBack callBack = new SchemaChangeCallBack();
    StructVector parent = new StructVector("root", allocator, new FieldType(false, Struct.INSTANCE, null, null), callBack);
    parent.allocateNew();
    IMemoryAllocator memoryAllocator = ArrowMemoryAllocatorFactory.getFromStructVector( ColumnType.DOUBLE , "target" , allocator , parent , 3 );

    maker.loadInMemoryStorage( columnBinary , memoryAllocator );

    StructReader rootReader = parent.getReader();
    FieldReader reader = rootReader.reader( "target" );
    reader.setPosition( 0 );
    assertEquals( reader.readDouble().doubleValue() , (double)100 );
    reader.setPosition( 1 );
    assertEquals( reader.readDouble().doubleValue() , (double)200 );
    reader.setPosition( 5 );
    assertEquals( reader.readDouble().doubleValue() , (double)255 );
    reader.setPosition( 2 );
    assertEquals( reader.readDouble() , null );
    reader.setPosition( 3 );
    assertEquals( reader.readDouble() , null );
    reader.setPosition( 4 );
    assertEquals( reader.readDouble() , null );
  }

}
