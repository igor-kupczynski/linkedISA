package org.isatools.isa2owl;

import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxOntologyFormat;
import org.isatools.isacreator.io.importisa.ISAtabFilesImporter;
import org.isatools.isacreator.io.importisa.ISAtabImporter;
import org.isatools.isacreator.model.*;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.SystemOutDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;


/**
 * It populates an ISA ontology with instances coming from ISATab files.
 * 
 * @author <a href="mailto:alejandra.gonzalez.beltran@gmail.com">Alejandra Gonzalez-Beltran</a>
 *
 */
public class ISAtab2OWLConverter {
	
	private static final Logger log = Logger.getLogger(ISAtab2OWLConverter.class);
	
	private ISAtabImporter importer = null;
	private String configDir = null;
    private ISASyntax2OWLMapping mapping = null;

    private OWLOntology ontology = null;
    private OWLOntologyManager manager = null;
    private OWLDataFactory factory = null;
    private IRI ontoIRI = null;

    //ontologies IRIs
    public static String BFO_IRI = "http://purl.obolibrary.org/bfo.owl";
    public static String OBI_IRI = "http://purl.obolibrary.org/obo/obi.owl";



    /**
	 * Constructor
	 * 
	 * @param cDir directory where the ISA configuration file can be found
	 */
	public ISAtab2OWLConverter(String cDir, ISASyntax2OWLMapping m){
		configDir = cDir;
        log.debug("configDir="+configDir);
        mapping = m;
		importer = new ISAtabFilesImporter(configDir);
		System.out.println("importer="+importer);
        manager = OWLManager.createOWLOntologyManager();
        factory = manager.getOWLDataFactory();

        try{
        //TODO add AutoIRIMapper
        //adding mapper for local ontologies
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create(ISAtab2OWLConverter.BFO_IRI), IRI.create(getClass().getClassLoader().getResource("owl/ruttenberg-bfo2.owl"))));
        manager.addIRIMapper(new SimpleIRIMapper(IRI.create(ISAtab2OWLConverter.OBI_IRI), IRI.create(getClass().getClassLoader().getResource("owl/obi.owl"))));


        ontology = manager.createOntology(ontoIRI);
        }catch(URISyntaxException e){
            e.printStackTrace();
        }catch(OWLOntologyCreationException e){
            e.printStackTrace();
        }


	}

    private void processSourceOntologies(){
        Map<String,IRI> sourceOntoIRIs = mapping.getSourceOntoIRIs();
        OWLOntology onto = null;

        //TODO check imports from ontologies where the import chain implies that ontologies are duplicated
        for(IRI iri: sourceOntoIRIs.values()){
            //try{
                System.out.println("iri="+iri);
                //onto = manager.loadOntology(iri);
                OWLImportsDeclaration importDecl = factory.getOWLImportsDeclaration(iri);
                manager.applyChange(new AddImport(ontology, importDecl));

            //}catch(OWLOntologyCreationException oocrex){
                //oocrex.printStackTrace();
            //}
        }

    }
	
	
	private boolean readInISAFiles(String parentDir){
		return importer.importFile(parentDir);
	}
	
	/**
	 * 
	 * @param parentDir
	 */
	public boolean populateOntology(String parentDir){
        log.debug("In populateOntology....");
		log.debug("parentDir=" + parentDir);
		if (!readInISAFiles(parentDir)){
            System.out.println(importer.getMessagesAsString());
        }

        processSourceOntologies();

		Investigation investigation = importer.getInvestigation();
        System.out.println("investigation=" + investigation);
        log.debug("investigation=" + investigation);
		Map<String,Study> studies = investigation.getStudies();
        System.out.println("number of studies=" + studies.keySet().size());
		for(String key: studies.keySet()){
			populateStudy(studies.get(key));
		}

        try{
        File file = new File("/Users/agbeltran/workspace-private/isa2owl/isatab-example.owl");
        manager.saveOntology(ontology, IRI.create(file.toURI()));

        // We can also dump an ontology to System.out by specifying a different OWLOntologyOutputTarget
        // Note that we can write an ontology to a stream in a similar way using the StreamOutputTarget class
        OWLOntologyDocumentTarget documentTarget = new SystemOutDocumentTarget();
        // Try another format - The Manchester OWL Syntax
        ManchesterOWLSyntaxOntologyFormat manSyntaxFormat = new ManchesterOWLSyntaxOntologyFormat();
        OWLXMLOntologyFormat format = new OWLXMLOntologyFormat();
        if(format.isPrefixOWLOntologyFormat()) {
            manSyntaxFormat.copyPrefixesFrom(format.asPrefixOWLOntologyFormat());
        }
        //save ontology
        manager.saveOntology(ontology, manSyntaxFormat, new SystemOutDocumentTarget());
        }catch(OWLOntologyStorageException e){
        e.printStackTrace();
        }

        return true;
	}
	
	private void populateStudy(Study study){
        System.out.println("study id"+study.getStudyId());
		System.out.println("study desc="+study.getStudyDesc());


        //Study
        createClassAssertion(ExtendedISASyntax.STUDY,study.getStudyId(),study.getStudyId());

        //Study identifier
        createClassAssertion(Study.STUDY_ID,study.getStudyId()+"_identifier",study.getStudyId());

        //properties for Study identifier
        //TODO

        //Study title
        createClassAssertion(Study.STUDY_TITLE,study.getStudyId()+"_title",study.getStudyTitle());

        //TODO add properties for title

        //Study description
        createClassAssertion(Study.STUDY_DESC,study.getStudyId()+"_description",study.getStudyDesc());

        //Study file name
        createClassAssertion(Study.STUDY_SAMPLE_FILE,study.getStudyId()+"_filename",study.getStudySampleFileIdentifier());


        //Publications
        List<Publication> publicationList = study.getPublications();
        convertPublications(study, publicationList);


        System.out.println("ASSAYS..." + study.getAssays());
		
	}

    private void convertPublications(Study study, List<Publication> publicationList){

        for(Publication pub: publicationList){
            StudyPublication publication = (StudyPublication) pub;

            //Publication
            createClassAssertion(ExtendedISASyntax.PUBLICATION,publication.getIdentifier(),publication.getIdentifier());

            //Study PubMed ID
            createClassAssertion(StudyPublication.PUBMED_ID,publication.getIdentifier()+"_pubmed", publication.getPubmedId());
        }

    }



    private OWLNamedIndividual createClassAssertion(String typeMappingLabel, String individualIdentifier, String individualLabel){


        IRI owlClassIRI = mapping.getTypeMapping(typeMappingLabel);
        if (owlClassIRI==null){
            System.err.println("No IRI for type " + typeMappingLabel);
            System.exit(-1);
        }
        OWLNamedIndividual individual = factory.getOWLNamedIndividual(ontoIRI.create(individualIdentifier));
        OWLAnnotation annotation = factory.getOWLAnnotation(factory.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),factory.getOWLLiteral(individualLabel));
        OWLAnnotationAssertionAxiom annotationAssertionAxiom = factory.getOWLAnnotationAssertionAxiom(individual.getIRI(), annotation);
        manager.addAxiom(ontology, annotationAssertionAxiom);

        OWLClass owlClass = factory.getOWLClass(owlClassIRI);
        OWLClassAssertionAxiom classAssertion = factory.getOWLClassAssertionAxiom(owlClass, individual);
        manager.addAxiom(ontology,classAssertion);

        return individual;
    }

    private void populateAssay(Assay assay){

    }

}
