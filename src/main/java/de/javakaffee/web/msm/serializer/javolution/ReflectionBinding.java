/*
 * Copyright 2009 Martin Grotzke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.javakaffee.web.msm.serializer.javolution;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.xml.XMLBinding;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

/**
 * An {@link XMLBinding} that provides class bindings based on reflection.
 * 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class ReflectionBinding extends XMLBinding {
    
    private static final Logger _log = Logger.getLogger( ReflectionBinding.class.getName() );
    
    private final Map<Class<?>, XMLFormat<?>> _formats = new ConcurrentHashMap<Class<?>, XMLFormat<?>>();

    private final ClassLoader _classLoader;
    private final XMLEnumFormat _enumFormat;
    private final XMLArrayFormat _arrayFormat;
    
    public ReflectionBinding(final ClassLoader classLoader) {
        _classLoader = classLoader;
        _enumFormat = new XMLEnumFormat( classLoader );
        _arrayFormat = new XMLArrayFormat( classLoader );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public <T> XMLFormat<T> getFormat(final Class<T> cls) {
        
        final XMLFormat<T> format = super.getFormat( cls );
        if ( cls.isPrimitive() || cls == String.class
                || cls == Boolean.class
                || cls == Integer.class
                || cls == Long.class
                || cls == Short.class
                || cls == Double.class
                || cls == Float.class
                || cls == Short.class
                || cls == Character.class
                || cls == Byte.class
                || Map.class.isAssignableFrom( cls ) || Collection.class.isAssignableFrom( cls ) ) {
            return format;
        }
        else if ( cls.isArray() ) {
            if ( cls == int[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.INT_ARRAY_FORMAT;
            }
            else if ( cls == long[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.LONG_ARRAY_FORMAT;
            }
            else if ( cls == short[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.SHORT_ARRAY_FORMAT;
            }
            else if ( cls == float[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.FLOAT_ARRAY_FORMAT;
            }
            else if ( cls == double[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.DOUBLE_ARRAY_FORMAT;
            }
            else if ( cls == char[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.CHAR_ARRAY_FORMAT;
            }
            else if ( cls == byte[].class ) {
                return (XMLFormat<T>) XMLArrayFormats.BYTE_ARRAY_FORMAT;
            }
            else {
                return (XMLFormat<T>) _arrayFormat;
            }
        }
        else if ( cls.isEnum() ) {
            return (XMLFormat<T>) _enumFormat;
        }
        else {
            XMLFormat<?> xmlFormat = _formats.get( cls );
            if ( xmlFormat == null ) {
                if ( Number.class.isAssignableFrom( cls ) ) {
                    xmlFormat = getNumberFormat( cls );
                }
                else {
                    xmlFormat = new ReflectionFormat( cls, _classLoader );
                }
                _formats.put( cls, xmlFormat );
            }
            return (XMLFormat<T>) xmlFormat;
        }
    }
    
    @SuppressWarnings( "unchecked" )
    static <T> XMLFormat<T> getNumberFormat( final Class<T> clazz ) {
        try {
            for( final Constructor<?> constructor : clazz.getConstructors() ) {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                if ( parameterTypes.length == 1 ) {
                    if ( parameterTypes[0] == long.class ) {
                        return new XMLNumberLongFormat<T>( (Constructor<T>) constructor );
                    }
                    if ( parameterTypes[0] == int.class ) {
                        return new XMLNumberIntFormat<T>( (Constructor<T>) constructor );
                    }
                }
            }
        } catch ( final Exception e ) {
            throw new RuntimeException( e );
        }
        throw new IllegalArgumentException( "No suitable constructor found for class " + clazz.getName() + ".\n" +
                "Available constructors: " + clazz.getConstructors() );
    }
    
    static class XMLNumberIntFormat<T> extends XMLFormat<T> {
        
        private final Constructor<T> _constructor;

        public XMLNumberIntFormat( final Constructor<T> constructor ) {
            _constructor = constructor;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public T newInstance( final Class<T> clazz, final javolution.xml.XMLFormat.InputElement xml ) throws XMLStreamException {
            final int value = xml.getAttribute( "value", 0 );
            try {
                return _constructor.newInstance( value );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement xml, final T obj ) throws XMLStreamException {
            // nothing to do...
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( final T obj, final javolution.xml.XMLFormat.OutputElement xml ) throws XMLStreamException {
            xml.setAttribute( "value", obj.toString() );
        }
        
    }
    
    static class XMLNumberLongFormat<T> extends XMLFormat<T> {
        
        private final Constructor<T> _constructor;

        public XMLNumberLongFormat( final Constructor<T> constructor ) {
            _constructor = constructor;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public T newInstance( final Class<T> clazz, final javolution.xml.XMLFormat.InputElement xml ) throws XMLStreamException {
            final long value = xml.getAttribute( "value", 0 );
            try {
                return _constructor.newInstance( value );
            } catch ( final Exception e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement xml, final T obj ) throws XMLStreamException {
            // nothing to do...
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( final T obj, final javolution.xml.XMLFormat.OutputElement xml ) throws XMLStreamException {
            xml.setAttribute( "value", obj.toString() );
        }
        
    }
    
    static class XMLEnumFormat extends XMLFormat<Enum<?>> {

        private final ClassLoader _classLoader;
        
        public XMLEnumFormat( final ClassLoader classLoader ) {
            _classLoader = classLoader;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Enum<?> newInstance( final Class<Enum<?>> clazz, final javolution.xml.XMLFormat.InputElement xml ) throws XMLStreamException {
            final String value = xml.getAttribute( "value", (String)null );
            final String clazzName = xml.getAttribute( "type", (String)null );
            try {
                @SuppressWarnings( "unchecked" )
                final Enum<?> enumValue = Enum.valueOf( Class.forName( clazzName ).asSubclass( Enum.class ), value );
                return enumValue;
            } catch ( final ClassNotFoundException e ) {
                throw new XMLStreamException( e );
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement xml, final Enum<?> object ) throws XMLStreamException {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void write( final Enum<?> object, final javolution.xml.XMLFormat.OutputElement xml ) throws XMLStreamException {
            xml.setAttribute( "value", object.name() );
            xml.setAttribute( "type", object.getClass().getName() );
        }
        
    }
    
    public abstract static class AbstractXMLArrayFormat<T> extends XMLFormat<T> {
        
        @Override
        public final void write( final T array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException {
            output.setAttribute( "type", "array" );
            output.setAttribute( "componentType", array.getClass().getComponentType().getName() );
            output.setAttribute("length", length( array ) );
            writeElements( array, output );
        }
        
        protected abstract int length( final T array );
        
        protected abstract void writeElements( final T array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException;
        
    }
    
    public static class XMLArrayFormat extends XMLFormat<Object[]> {

        private final ClassLoader _classLoader;
        
        public XMLArrayFormat(final ClassLoader classLoader) {
            _classLoader = classLoader;
        }
        
        /**
         * {@inheritDoc}
         */
        @SuppressWarnings( "unchecked" )
        @Override
        public Object[] newInstance( final Class clazz, final javolution.xml.XMLFormat.InputElement input ) throws XMLStreamException {
            try {
                final String componentType = input.getAttribute( "componentType", (String)null );
                final int length = input.getAttribute( "length", 0 );
                return (Object[]) Array.newInstance( Class.forName( componentType, false, _classLoader ) , length );
            } catch ( final Exception e ) {
                _log.log( Level.SEVERE, "caught exception", e );
                throw new XMLStreamException( e );
            }
        }
        
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement input, final Object[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getNext();
            }
        }
        
        @Override
        public final void write( final Object[] array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException {
            output.setAttribute( "type", "array" );
            output.setAttribute( "componentType", array.getClass().getComponentType().getName() );
            output.setAttribute("length", array.length );
            writeElements( array, output );
        }
        
        public void writeElements( final Object[] array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException {
            for( final Object item : array ) {
                output.add( item );
            }
        }
        
    }
    
    public static class XMLByteArrayFormat extends XMLFormat<byte[]> {
        
        @Override
        public byte[] newInstance( final Class<byte[]> clazz, final javolution.xml.XMLFormat.InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (byte[]) Array.newInstance( byte.class, length );
            } catch ( final Exception e ) {
                _log.log( Level.SEVERE, "caught exception", e );
                throw new XMLStreamException( e );
            }
        }
        
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement input, final byte[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getNext();
            }
        }
        
        @Override
        public final void write( final byte[] array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException {
            output.setAttribute("length", array.length );
            for( final byte item : array ) {
                output.add( item );
            }
        }
        
    }
    
    public static class XMLCharArrayFormat extends XMLFormat<char[]> {
        
        @Override
        public char[] newInstance( final Class<char[]> clazz, final javolution.xml.XMLFormat.InputElement input ) throws XMLStreamException {
            try {
                final int length = input.getAttribute( "length", 0 );
                return (char[]) Array.newInstance( char.class, length );
            } catch ( final Exception e ) {
                _log.log( Level.SEVERE, "caught exception", e );
                throw new XMLStreamException( e );
            }
        }
        
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement input, final char[] array ) throws XMLStreamException {
            int i = 0;
            while ( input.hasNext() ) {
                array[i++] = input.getNext();
            }
        }
        
        @Override
        public final void write( final char[] array, final javolution.xml.XMLFormat.OutputElement output ) throws XMLStreamException {
            output.setAttribute("length", array.length );
            for( final char item : array ) {
                output.add( item );
            };
        }
        
    }
    
    public static class XMLCollectionFormat extends XMLFormat<Collection<Object>> {
        
        @Override
        public void read( final javolution.xml.XMLFormat.InputElement xml, final Collection<Object> obj ) throws XMLStreamException {
            while ( xml.hasNext() ) {
                obj.add( xml.getNext() );
            }
        }
        
        @Override
        public void write( final Collection<Object> obj, final javolution.xml.XMLFormat.OutputElement xml ) throws XMLStreamException {
            for( final Object item : obj ) {
                xml.add( item );
            }
        }
        
    }
    
}