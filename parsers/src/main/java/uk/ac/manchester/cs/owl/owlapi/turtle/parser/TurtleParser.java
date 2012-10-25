package uk.ac.manchester.cs.owl.owlapi.turtle.parser;

import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import org.coode.owlapi.rdfxml.parser.AnonymousNodeChecker;
import org.coode.string.EscapeUtils;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import uk.ac.manchester.cs.JavaCharStream;

@SuppressWarnings("javadoc")
public class TurtleParser implements AnonymousNodeChecker, TurtleParserConstants {

    private Map<String, IRI> string2IRI;

    private String base;

    private int blankNodeId;

    private TripleHandler handler;

    private DefaultPrefixManager pm = new DefaultPrefixManager();


    public TurtleParser(Reader reader, TripleHandler handler, String base) {
        this(reader);
        this.handler = handler;
        this.base = base;
        string2IRI = new HashMap<String, IRI>();
        blankNodeId = 0;
        pm.setDefaultPrefix("http://www.semanticweb.org/owl/owlapi/turtle#");
    }

    public TurtleParser(InputStream is, TripleHandler handler, String base) {
        this(is);
        this.handler = handler;
        this.base = base;
        string2IRI = new HashMap<String, IRI>();
        blankNodeId = 0;
        pm.setDefaultPrefix("http://www.semanticweb.org/owl/owlapi/turtle#");
    }

    public DefaultPrefixManager getPrefixManager() {
        return pm;
    }

    public void setTripleHandler(TripleHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean isAnonymousNode(String iri) {
        return iri.indexOf("genid") != -1;
    }


    @Override
    public boolean isAnonymousNode(IRI iri) {
        return iri.toString().indexOf("genid") != -1;
    }

    @Override
    public boolean isAnonymousSharedNode(String iri) {
        return iri.indexOf("genid-nodeid") != -1;
    }

    protected IRI getNextBlankNode(String id) {
        IRI iri;
        if(id == null) {
            iri = getIRI("genid" + blankNodeId);
            blankNodeId++;
        }
        else {
            iri = getIRI("genid-nodeid-" + id);
        }

        return iri;
    }

    protected IRI getIRIFromQName(String qname) throws ParseException  {
        int colonIndex = qname.indexOf(':');
        if(colonIndex == -1) {
            throw new ParseException("Not a valid qname (missing ':') " + qname);
        }
        String prefix = qname.substring(0, colonIndex + 1);
        if(prefix.equals("_:")) {
            return getIRI("genid" + qname.substring(colonIndex + 1));
        }
        if(!pm.containsPrefixMapping(prefix)) {
            throw new ParseException("Prefix not declared: " + prefix);
        }
        return pm.getIRI(qname);
    }

    public IRI getIRI(String s) {
        if(s.charAt(0) == '<') {
            s = s.substring(1, s.length() - 1);
        }
        IRI iri = string2IRI.get(s);
        if(iri == null) {
            iri = IRI.create(s);
            if (!iri.isAbsolute()) {
                iri = IRI.create(base.substring(0, base.lastIndexOf('/') + 1) + s);
            }
            string2IRI.put(s, iri);
        }
        return iri;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    //TOKEN:
    //{
    //    <LONG_STRING: (<QUOTE><QUOTE><QUOTE>~["\""]<QUOTE><QUOTE><QUOTE>)>
    //}
    final public void parseDocument() throws ParseException {
        label_1:
            while (true) {
                if (jj_2_1(2)) {
                    parseDirective();
                    jj_consume_token(DOT);
                } else if (jj_2_2(2)) {
                    parseStatement();
                    jj_consume_token(DOT);
                } else {
                    jj_consume_token(-1);
                    throw new ParseException();
                }
                if (jj_2_3(2)) {

                } else {
                    break label_1;
                }
            }
    jj_consume_token(0);
    handler.handleEnd();
    }

    final public void parseDirective() throws ParseException {
        if (jj_2_4(2)) {
            parsePrefixDirective();
        } else if (jj_2_5(2)) {
            parseBaseDirective();
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
    }

    final public void parsePrefixDirective() throws ParseException {
        Token t;
        String prefix = "";
        IRI ns;
        jj_consume_token(PREFIX);
        t = jj_consume_token(PNAME_NS);
        prefix=t.image;
        ns = parseIRI();
        pm.setPrefix(prefix, ns.toString());
        handler.handlePrefixDirective(prefix, ns.toString());
    }

    final public void parseBaseDirective() throws ParseException {
        Token t;
        jj_consume_token(BASE);
        t = jj_consume_token(FULLIRI);
        base = t.image.substring(1, t.image.length() - 1);
        handler.handleBaseDirective(base);
    }

    final public void parseStatement() throws ParseException {
        parseTriples();
    }

    final public void parseTriples() throws ParseException {
        IRI subject;
        subject = parseSubject();
        if (jj_2_6(2)) {
            parsePredicateObjectList(subject);
        }
    }

    final public IRI parseSubject() throws ParseException {
        IRI iri;
        if (jj_2_7(2)) {
            iri = parseResource();
        } else if (jj_2_8(2)) {
            iri = parseBlankNode();
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return iri;
    }

    final public IRI parseLoneNS() throws ParseException {
        Token t;
        t = jj_consume_token(PNAME_NS);
                return getIRIFromQName(t.image);
    }

    final public IRI parseAbbreviatedIRI() throws ParseException {
        Token t;
        t = jj_consume_token(PNAME_LN);
            return getIRIFromQName(t.image);
    }

    final public IRI parseIRI() throws ParseException {
        Token t;
        t = jj_consume_token(FULLIRI);
            return getIRI(t.image);
    }

    final public IRI parseBlankNode() throws ParseException {
        IRI iri = null;
        if (jj_2_11(2)) {
            iri = parseNodeID();
        } else if (jj_2_12(2)) {
            jj_consume_token(EMPTY_BLANK_NODE);
            if(iri==null){iri = getNextBlankNode(null);}
            parsePredicateObjectList(iri);
        } else if (jj_2_13(2)) {
            jj_consume_token(OPEN_SQUARE_BRACKET);
            if (jj_2_10(2)) {
                if(iri==null){iri = getNextBlankNode(null);}
                parsePredicateObjectList(iri);
                if (jj_2_9(2)) {
                    jj_consume_token(DOT);
                }
            }
            jj_consume_token(CLOSE_SQUARE_BRACKET);
            if (iri == null) {iri = getNextBlankNode(null); }
        } else if (jj_2_14(2)) {
            iri = parseCollection();
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return iri;
    }

    final public IRI parseNodeID() throws ParseException {
        Token t;
        t = jj_consume_token(NODEID);
            return getNextBlankNode(t.image);
    }

    final public void parsePredicateObjectList(IRI subject) throws ParseException {
        IRI predicate;
        predicate = parseVerb();
        parseObjectList(subject, predicate);
        label_2:
            while (true) {
                if (jj_2_15(2)) {

                } else {
                    break label_2;
                }
                jj_consume_token(SEMICOLON);
                predicate = parseVerb();
                parseObjectList(subject, predicate);
            }
        if (jj_2_16(2)) {
            jj_consume_token(SEMICOLON);
        }
    }

    final public IRI parseVerb() throws ParseException {
        IRI iri;
        if (jj_2_17(2)) {
            jj_consume_token(A);
            iri = OWLRDFVocabulary.RDF_TYPE.getIRI();
        } else if (jj_2_18(2)) {
            iri = parsePredicate();
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return iri;
    }

    final public IRI parsePredicate() throws ParseException {
        IRI iri;
        iri = parseResource();
            return iri;
    }

    final public IRI parseResource() throws ParseException {
        IRI iri;
        if (jj_2_19(2)) {
            iri = parseIRI();
        } else if (jj_2_20(2)) {
            iri = parseAbbreviatedIRI();
        } else if (jj_2_21(2)) {
            iri = parseLoneNS();
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return iri;
    }

    final public void parseObjectList(IRI subject, IRI predicate) throws ParseException {
        parseObject(subject, predicate);
        label_3:
            while (true) {
                if (jj_2_22(2)) {
                } else {
                    break label_3;
                }
                jj_consume_token(COMMA);
                parseObject(subject, predicate);
            }
    }

    final public void parseObject(IRI subject, IRI predicate) throws ParseException {
        IRI resObject;
        if (jj_2_25(2)) {
            parseLiteral(subject, predicate);
        } else if (jj_2_26(2)) {
            if (jj_2_23(2)) {
                resObject = parseResource();
            } else if (jj_2_24(2)) {
                resObject = parseBlankNode();
            } else {
                jj_consume_token(-1);
                throw new ParseException();
            }
            handler.handleTriple(subject, predicate, resObject);
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
    }

    final public IRI parseCollection() throws ParseException {
        IRI iri;
        jj_consume_token(OPENPAR);
        iri = parseItemList();
        jj_consume_token(CLOSEPAR);
            return iri;
    }

    final public IRI parseItemList() throws ParseException {
        //  _x  rdf:type rdf:List
        //  _x  rdf:first
        //  _x  rdf:next
        IRI firstSubject = OWLRDFVocabulary.RDF_NIL.getIRI();
        IRI subject = null;
        IRI type = OWLRDFVocabulary.RDF_TYPE.getIRI();
        IRI first = OWLRDFVocabulary.RDF_FIRST.getIRI();
        IRI rest = OWLRDFVocabulary.RDF_REST.getIRI();
        IRI list = OWLRDFVocabulary.RDF_LIST.getIRI();
        IRI nil = OWLRDFVocabulary.RDF_NIL.getIRI();
        label_4:
            while (true) {
                if (jj_2_27(2)) {

                } else {
                    break label_4;
                }
                IRI prevSubject = subject;
                subject=getNextBlankNode(null);
                if(prevSubject != null) {
                    handler.handleTriple(prevSubject, rest, subject);
                }
                else {
                    firstSubject = subject;
                }
                if(subject!=null) {
                    handler.handleTriple(subject, type, list);
                }
                parseObject(subject, first);
            }
        // Terminate list
        if(subject!=null) {
            handler.handleTriple(subject, rest, nil);
        }
            return firstSubject;
    }

    final public void parseLiteral(IRI subject, IRI predicate) throws ParseException {
        String literal;
        String lang = null;
        IRI datatype = null;
        Token t;
        if (jj_2_31(2)) {
            literal = parseQuotedString();
            if (jj_2_30(2)) {
                if (jj_2_28(2)) {
                    jj_consume_token(DOUBLE_CARET);
                    datatype = parseResource();
                } else if (jj_2_29(2)) {
                    jj_consume_token(AT);
                    t = jj_consume_token(PN_LOCAL);
                    lang=t.image;
                } else {
                    jj_consume_token(-1);
                    throw new ParseException();
                }
            }
            if(datatype != null) {
                handler.handleTriple(subject, predicate, literal, datatype);
            }
            else if(lang != null) {
                handler.handleTriple(subject, predicate, literal, lang);
            }
            else {
                handler.handleTriple(subject, predicate, literal);
            }
        } else if (jj_2_32(2)) {
            literal = parseInteger();
            handler.handleTriple(subject, predicate, literal, XSDVocabulary.INTEGER.getIRI());
        } else if (jj_2_33(2)) {
            literal = parseDouble();
            handler.handleTriple(subject, predicate, literal, XSDVocabulary.DOUBLE.getIRI());
        } else if (jj_2_34(2)) {
            literal = parseDecimal();
            handler.handleTriple(subject, predicate, literal, XSDVocabulary.DECIMAL.getIRI());
        } else if (jj_2_35(2)) {
            literal = parseBoolean();
            handler.handleTriple(subject, predicate, literal, XSDVocabulary.BOOLEAN.getIRI());
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
    }

    final public String parseInteger() throws ParseException {
        Token t;
        if (jj_2_36(2)) {
            t = jj_consume_token(INTEGER);
                return t.image;
        } else if (jj_2_37(2)) {
            t = jj_consume_token(DIGIT);
                return t.image;
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
    }

    final public String parseDouble() throws ParseException {
        Token t;
        t = jj_consume_token(DOUBLE);
            return t.image;
    }

    final public String parseDecimal() throws ParseException {
        Token t;
        t = jj_consume_token(DECIMAL);
            return t.image;
    }

    final public String parseBoolean() throws ParseException {
        Token t;
        if (jj_2_38(2)) {
            t = jj_consume_token(TRUE);
        } else if (jj_2_39(2)) {
            t = jj_consume_token(FALSE);
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return t.image;
    }

    final public String parseQuotedString() throws ParseException {
        String s;
        s = parseString();
            return s;
    }

    final public String parseString() throws ParseException {
        Token t;
        String rawString = "";
        if (jj_2_40(2)) {
            t = jj_consume_token(STRING);
            rawString = t.image.substring(1, t.image.length() - 1);
        } else if (jj_2_41(2)) {
            t = jj_consume_token(LONG_STRING);
            rawString = t.image.substring(3, t.image.length() - 3);
        } else {
            jj_consume_token(-1);
            throw new ParseException();
        }
            return EscapeUtils.unescapeString(rawString);
    }

    private boolean jj_2_1(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_1(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(0, xla); }
    }

    private boolean jj_2_2(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_2(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(1, xla); }
    }

    private boolean jj_2_3(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_3(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(2, xla); }
    }

    private boolean jj_2_4(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_4(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(3, xla); }
    }

    private boolean jj_2_5(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_5(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(4, xla); }
    }

    private boolean jj_2_6(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_6(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(5, xla); }
    }

    private boolean jj_2_7(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_7(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(6, xla); }
    }

    private boolean jj_2_8(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_8(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(7, xla); }
    }

    private boolean jj_2_9(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_9(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(8, xla); }
    }

    private boolean jj_2_10(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_10(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(9, xla); }
    }

    private boolean jj_2_11(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_11(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(10, xla); }
    }

    private boolean jj_2_12(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_12(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(11, xla); }
    }

    private boolean jj_2_13(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_13(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(12, xla); }
    }

    private boolean jj_2_14(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_14(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(13, xla); }
    }

    private boolean jj_2_15(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_15(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(14, xla); }
    }

    private boolean jj_2_16(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_16(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(15, xla); }
    }

    private boolean jj_2_17(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_17(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(16, xla); }
    }

    private boolean jj_2_18(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_18(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(17, xla); }
    }

    private boolean jj_2_19(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_19(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(18, xla); }
    }

    private boolean jj_2_20(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_20(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(19, xla); }
    }

    private boolean jj_2_21(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_21(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(20, xla); }
    }

    private boolean jj_2_22(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_22(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(21, xla); }
    }

    private boolean jj_2_23(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_23(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(22, xla); }
    }

    private boolean jj_2_24(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_24(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(23, xla); }
    }

    private boolean jj_2_25(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_25(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(24, xla); }
    }

    private boolean jj_2_26(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_26(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(25, xla); }
    }

    private boolean jj_2_27(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_27(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(26, xla); }
    }

    private boolean jj_2_28(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_28(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(27, xla); }
    }

    private boolean jj_2_29(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_29(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(28, xla); }
    }

    private boolean jj_2_30(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_30(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(29, xla); }
    }

    private boolean jj_2_31(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_31(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(30, xla); }
    }

    private boolean jj_2_32(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_32(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(31, xla); }
    }

    private boolean jj_2_33(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_33(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(32, xla); }
    }

    private boolean jj_2_34(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_34(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(33, xla); }
    }

    private boolean jj_2_35(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_35(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(34, xla); }
    }

    private boolean jj_2_36(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_36(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(35, xla); }
    }

    private boolean jj_2_37(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_37(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(36, xla); }
    }

    private boolean jj_2_38(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_38(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(37, xla); }
    }

    private boolean jj_2_39(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_39(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(38, xla); }
    }

    private boolean jj_2_40(int xla) {
        jj_la = xla; jj_lastpos = jj_scanpos = token;
        try { return !jj_3_40(); }
        catch(LookaheadSuccess ls) { return true; }
        finally { jj_save(39, xla); }
    }

    private boolean jj_2_41(int xla) {
        jj_la = xla;
        jj_lastpos = jj_scanpos = token;
        try {
            return !jj_3_41();
        } catch (LookaheadSuccess ls) {
            return true;
        } finally {
            jj_save(40, xla);
        }
    }

    private boolean jj_3_6() {
        if (jj_3R_9()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_16() {
        if (jj_scan_token(SEMICOLON)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_27() {
        if (jj_3R_19()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_18() {
        if (jj_scan_token(PNAME_NS)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_35() {
        if (jj_3R_25()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_34() {
        if (jj_3R_24()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_33() {
        if (jj_3R_23()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_32() {
        if (jj_3R_22()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_19() {
        if (jj_3R_16()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_10() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_19()) {
            jj_scanpos = xsp;
            if (jj_3_20()) {
                jj_scanpos = xsp;
                if (jj_3_21()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean jj_3_7() {
        if (jj_3R_10()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_30() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_7()) {
            jj_scanpos = xsp;
            if (jj_3_8()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3R_26() {
        if (jj_3R_30()) {
            return true;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_6()) {
            jj_scanpos = xsp;
        }
        return false;
    }

    private boolean jj_3R_15() {
        if (jj_3R_10()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_20() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_31()) {
            jj_scanpos = xsp;
            if (jj_3_32()) {
                jj_scanpos = xsp;
                if (jj_3_33()) {
                    jj_scanpos = xsp;
                    if (jj_3_34()) {
                        jj_scanpos = xsp;
                        if (jj_3_35()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean jj_3_31() {
        if (jj_3R_21()) {
            return true;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_30()) {
            jj_scanpos = xsp;
        }
        return false;
    }

    private boolean jj_3_41() {
        if (jj_scan_token(LONG_STRING)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_6() {
        if (jj_3R_26()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_40() {
        if (jj_scan_token(STRING)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_17() {
        if (jj_scan_token(A)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_29() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_40()) {
            jj_scanpos = xsp;
            if (jj_3_41()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3R_14() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_17()) {
            jj_scanpos = xsp;
            if (jj_3_18()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_5() {
        if (jj_3R_8()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_8() {
        if (jj_scan_token(BASE)) {
            return true;
        }
        if (jj_scan_token(FULLIRI)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_10() {
        if (jj_3R_9()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_9() {
        if (jj_3R_14()) {
            return true;
        }
        if (jj_3R_27()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_39() {
        if (jj_scan_token(FALSE)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_2() {
        if (jj_3R_6()) {
            return true;
        }
        if (jj_scan_token(DOT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_21() {
        if (jj_3R_29()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_29() {
        if (jj_scan_token(AT)) {
            return true;
        }
        if (jj_scan_token(PN_LOCAL)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_12() {
        if (jj_scan_token(NODEID)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_7() {
        if (jj_scan_token(PREFIX)) {
            return true;
        }
        if (jj_scan_token(PNAME_NS)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_21() {
        if (jj_3R_18()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_38() {
        if (jj_scan_token(TRUE)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_25() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_38()) {
            jj_scanpos = xsp;
            if (jj_3_39()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_27() {
        if (jj_3R_19()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_28() {
        Token xsp;
        while (true) {
            xsp = jj_scanpos;
            if (jj_3_27()) {
                jj_scanpos = xsp;
                break;
            }
        }
        return false;
    }

    private boolean jj_3_9() {
        if (jj_scan_token(DOT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_5() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_4()) {
            jj_scanpos = xsp;
            if (jj_3_5()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_4() {
        if (jj_3R_7()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_14() {
        if (jj_3R_13()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_13() {
        if (jj_scan_token(OPEN_SQUARE_BRACKET)) {
            return true;
        }
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_10()) {
            jj_scanpos = xsp;
        }
        if (jj_scan_token(CLOSE_SQUARE_BRACKET)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_22() {
        if (jj_scan_token(COMMA)) {
            return true;
        }
        if (jj_3R_19()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_24() {
        if (jj_scan_token(DECIMAL)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_12() {
        if (jj_scan_token(EMPTY_BLANK_NODE)) {
            return true;
        }
        if (jj_3R_9()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_1() {
        if (jj_3R_5()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_3() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_1()) {
            jj_scanpos = xsp;
            if (jj_3_2()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_11() {
        if (jj_3R_12()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_11() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_11()) {
            jj_scanpos = xsp;
            if (jj_3_12()) {
                jj_scanpos = xsp;
                if (jj_3_13()) {
                    jj_scanpos = xsp;
                    if (jj_3_14()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean jj_3R_23() {
        if (jj_scan_token(DOUBLE)) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_13() {
        if (jj_scan_token(OPENPAR)) {
            return true;
        }
        if (jj_3R_28()) {
            return true;
        }
        if (jj_scan_token(CLOSEPAR)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_15() {
        if (jj_scan_token(SEMICOLON)) {
            return true;
        }
        if (jj_3R_14()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_16() {
        if (jj_scan_token(FULLIRI)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_18() {
        if (jj_3R_15()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_37() {
        if (jj_scan_token(DIGIT)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_23() {
        if (jj_3R_10()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_24() {
        if (jj_3R_11()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_8() {
        if (jj_3R_11()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_26() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_23()) {
            jj_scanpos = xsp;
            if (jj_3_24()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3R_17() {
        if (jj_scan_token(PNAME_LN)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_25() {
        if (jj_3R_20()) {
            return true;
        }
        return false;
    }

    private boolean jj_3R_22() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_36()) {
            jj_scanpos = xsp;
            if (jj_3_37()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3R_19() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_25()) {
            jj_scanpos = xsp;
            if (jj_3_26()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_36() {
        if (jj_scan_token(INTEGER)) {
            return true;
        }
        return false;
    }

    private boolean jj_3_28() {
        if (jj_scan_token(DOUBLE_CARET)) {
            return true;
        }
        if (jj_3R_10()) {
            return true;
        }
        return false;
    }

    private boolean jj_3_30() {
        Token xsp;
        xsp = jj_scanpos;
        if (jj_3_28()) {
            jj_scanpos = xsp;
            if (jj_3_29()) {
                return true;
            }
        }
        return false;
    }

    private boolean jj_3_20() {
        if (jj_3R_17()) {
            return true;
        }
        return false;
    }

    /** Generated Token Manager. */
    public TurtleParserTokenManager token_source;
    JavaCharStream jj_input_stream;
    /** Current token. */
    public Token token;
    /** Next token. */
    public Token jj_nt;
    private int jj_ntk;
    private Token jj_scanpos, jj_lastpos;
    private int jj_la;
    private int jj_gen;
    final private int[] jj_la1 = new int[0];
    static private int[] jj_la1_0;
    static private int[] jj_la1_1;
    static {
        jj_la1_init_0();
        jj_la1_init_1();
    }
    private static void jj_la1_init_0() {
        jj_la1_0 = new int[] {};
    }
    private static void jj_la1_init_1() {
        jj_la1_1 = new int[] {};
    }

    final private JJCalls[] jj_2_rtns = new JJCalls[41];
    private boolean jj_rescan = false;
    private int jj_gc = 0;

    /** Constructor with InputStream. */
    public TurtleParser(java.io.InputStream stream) {
        this(stream, null);
    }
    /** Constructor with InputStream and supplied encoding */
    public TurtleParser(java.io.InputStream stream, String encoding) {
        try { jj_input_stream = new JavaCharStream(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
        token_source = new TurtleParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /** Reinitialise. */
    public void ReInit(java.io.InputStream stream) {
        ReInit(stream, null);
    }
    /** Reinitialise. */
    public void ReInit(java.io.InputStream stream, String encoding) {
        try { jj_input_stream.ReInit(stream, encoding, 1, 1); } catch(java.io.UnsupportedEncodingException e) { throw new RuntimeException(e); }
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /** Constructor. */
    public TurtleParser(java.io.Reader stream) {
        jj_input_stream = new JavaCharStream(stream, 1, 1);
        token_source = new TurtleParserTokenManager(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /** Reinitialise. */
    public void ReInit(java.io.Reader stream) {
        jj_input_stream.ReInit(stream, 1, 1);
        token_source.ReInit(jj_input_stream);
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /** Constructor with generated Token Manager. */
    public TurtleParser(TurtleParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    /** Reinitialise. */
    public void ReInit(TurtleParserTokenManager tm) {
        token_source = tm;
        token = new Token();
        jj_ntk = -1;
        jj_gen = 0;
        for (int i = 0; i < 0; i++) {
            jj_la1[i] = -1;
        }
        for (int i = 0; i < jj_2_rtns.length; i++) {
            jj_2_rtns[i] = new JJCalls();
        }
    }

    private Token jj_consume_token(int kind) throws ParseException {
        Token oldToken;
        if ((oldToken = token).next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        if (token.kind == kind) {
            jj_gen++;
            if (++jj_gc > 100) {
                jj_gc = 0;
                for (int i = 0; i < jj_2_rtns.length; i++) {
                    JJCalls c = jj_2_rtns[i];
                    while (c != null) {
                        if (c.gen < jj_gen) {
                            c.first = null;
                        }
                        c = c.next;
                    }
                }
            }
            return token;
        }
        token = oldToken;
        jj_kind = kind;
        throw generateParseException();
    }

    static private final class LookaheadSuccess extends java.lang.Error {
        public LookaheadSuccess() {}
    }
    final private LookaheadSuccess jj_ls = new LookaheadSuccess();
    private boolean jj_scan_token(int kind) {
        if (jj_scanpos == jj_lastpos) {
            jj_la--;
            if (jj_scanpos.next == null) {
                jj_lastpos = jj_scanpos = jj_scanpos.next = token_source.getNextToken();
            } else {
                jj_lastpos = jj_scanpos = jj_scanpos.next;
            }
        } else {
            jj_scanpos = jj_scanpos.next;
        }
        if (jj_rescan) {
            int i = 0; Token tok = token;
            while (tok != null && tok != jj_scanpos) { i++; tok = tok.next; }
            if (tok != null) {
                jj_add_error_token(kind, i);
            }
        }
        if (jj_scanpos.kind != kind) {
            return true;
        }
        if (jj_la == 0 && jj_scanpos == jj_lastpos) {
            throw jj_ls;
        }
        return false;
    }


    /** Get the next Token. */
    final public Token getNextToken() {
        if (token.next != null) {
            token = token.next;
        } else {
            token = token.next = token_source.getNextToken();
        }
        jj_ntk = -1;
        jj_gen++;
        return token;
    }

    /** Get the specific Token. */
    final public Token getToken(int index) {
        Token t = token;
        for (int i = 0; i < index; i++) {
            if (t.next != null) {
                t = t.next;
            } else {
                t = t.next = token_source.getNextToken();
            }
        }
        return t;
    }


    private java.util.List<int[]> jj_expentries = new java.util.ArrayList<int[]>();
    private int[] jj_expentry;
    private int jj_kind = -1;
    private int[] jj_lasttokens = new int[100];
    private int jj_endpos;

    private void jj_add_error_token(int kind, int pos) {
        if (pos >= 100) {
            return;
        }
        if (pos == jj_endpos + 1) {
            jj_lasttokens[jj_endpos++] = kind;
        } else if (jj_endpos != 0) {
            jj_expentry = new int[jj_endpos];
            for (int i = 0; i < jj_endpos; i++) {
                jj_expentry[i] = jj_lasttokens[i];
            }
            jj_entries_loop: for (java.util.Iterator<?> it = jj_expentries.iterator(); it.hasNext();) {
                int[] oldentry = (int[])it.next();
                if (oldentry.length == jj_expentry.length) {
                    for (int i = 0; i < jj_expentry.length; i++) {
                        if (oldentry[i] != jj_expentry[i]) {
                            continue jj_entries_loop;
                        }
                    }
                    jj_expentries.add(jj_expentry);
                    break jj_entries_loop;
                }
            }
            if (pos != 0) {
                jj_lasttokens[(jj_endpos = pos) - 1] = kind;
            }
        }
    }

    /** Generate ParseException. */
    public ParseException generateParseException() {
        jj_expentries.clear();
        boolean[] la1tokens = new boolean[48];
        if (jj_kind >= 0) {
            la1tokens[jj_kind] = true;
            jj_kind = -1;
        }
        for (int i = 0; i < 0; i++) {
            if (jj_la1[i] == jj_gen) {
                for (int j = 0; j < 32; j++) {
                    if ((jj_la1_0[i] & 1<<j) != 0) {
                        la1tokens[j] = true;
                    }
                    if ((jj_la1_1[i] & 1<<j) != 0) {
                        la1tokens[32+j] = true;
                    }
                }
            }
        }
        for (int i = 0; i < 48; i++) {
            if (la1tokens[i]) {
                jj_expentry = new int[1];
                jj_expentry[0] = i;
                jj_expentries.add(jj_expentry);
            }
        }
        jj_endpos = 0;
        jj_rescan_token();
        jj_add_error_token(0, 0);
        int[][] exptokseq = new int[jj_expentries.size()][];
        for (int i = 0; i < jj_expentries.size(); i++) {
            exptokseq[i] = jj_expentries.get(i);
        }
        return new ParseException(token, exptokseq, tokenImage);
    }

    /** Enable tracing. */
    final public void enable_tracing() {
    }

    /** Disable tracing. */
    final public void disable_tracing() {
    }

    private void jj_rescan_token() {
        jj_rescan = true;
        for (int i = 0; i < 41; i++) {
            try {
                JJCalls p = jj_2_rtns[i];
                do {
                    if (p.gen > jj_gen) {
                        jj_la = p.arg; jj_lastpos = jj_scanpos = p.first;
                        switch (i) {
                            case 0: jj_3_1(); break;
                            case 1: jj_3_2(); break;
                            case 2: jj_3_3(); break;
                            case 3: jj_3_4(); break;
                            case 4: jj_3_5(); break;
                            case 5: jj_3_6(); break;
                            case 6: jj_3_7(); break;
                            case 7: jj_3_8(); break;
                            case 8: jj_3_9(); break;
                            case 9: jj_3_10(); break;
                            case 10: jj_3_11(); break;
                            case 11: jj_3_12(); break;
                            case 12: jj_3_13(); break;
                            case 13: jj_3_14(); break;
                            case 14: jj_3_15(); break;
                            case 15: jj_3_16(); break;
                            case 16: jj_3_17(); break;
                            case 17: jj_3_18(); break;
                            case 18: jj_3_19(); break;
                            case 19: jj_3_20(); break;
                            case 20: jj_3_21(); break;
                            case 21: jj_3_22(); break;
                            case 22: jj_3_23(); break;
                            case 23: jj_3_24(); break;
                            case 24: jj_3_25(); break;
                            case 25: jj_3_26(); break;
                            case 26: jj_3_27(); break;
                            case 27: jj_3_28(); break;
                            case 28: jj_3_29(); break;
                            case 29: jj_3_30(); break;
                            case 30: jj_3_31(); break;
                            case 31: jj_3_32(); break;
                            case 32: jj_3_33(); break;
                            case 33: jj_3_34(); break;
                            case 34: jj_3_35(); break;
                            case 35: jj_3_36(); break;
                            case 36: jj_3_37(); break;
                            case 37: jj_3_38(); break;
                            case 38: jj_3_39(); break;
                            case 39: jj_3_40(); break;
                            case 40:
                                jj_3_41();
                                break;
                            default:
                                break;
                        }
                    }
                    p = p.next;
                } while (p != null);
            } catch(LookaheadSuccess ls) { }
        }
        jj_rescan = false;
    }

    private void jj_save(int index, int xla) {
        JJCalls p = jj_2_rtns[index];
        while (p.gen > jj_gen) {
            if (p.next == null) { p = p.next = new JJCalls(); break; }
            p = p.next;
        }
        p.gen = jj_gen + xla - jj_la; p.first = token; p.arg = xla;
    }

    static final class JJCalls {
        int gen;
        Token first;
        int arg;
        JJCalls next;
    }

}
