package org.isatools.graph.model;

import org.isatools.isa2owl.converter.ExtendedISASyntax;
import org.isatools.isacreator.model.GeneralFieldTypes;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by the ISATeam.
 * User: agbeltran
 * Date: 06/11/2012
 * Time: 16:11
 *
 * @author <a href="mailto:alejandra.gonzalez.beltran@gmail.com">Alejandra Gonzalez-Beltran</a>
 */
public class MaterialNode extends Node {

    //TODO change Node to MaterialAttribute?
    private List<Node> materialAttributes;

    public MaterialNode(int index, String name) {
        super(index, name);
        materialAttributes = new ArrayList<Node>();
    }

    public void addMaterialAttribute(Node property) {
        materialAttributes.add(property);
    }

    public List<Node> getMaterialAttributes() {
        return materialAttributes;
    }

    public String getMaterialNodeType(){

        System.out.println("MATERIAL NODE NAME "+getName());

        if (getName().equals(GeneralFieldTypes.SAMPLE_NAME)){
            return ExtendedISASyntax.SAMPLE;
        }
        if (getName().equals(GeneralFieldTypes.SOURCE_NAME)){
            return ExtendedISASyntax.SOURCE;
        }
        return null;
    }

    public String getMaterialNodeName(){
        return getName().substring(0,getName().length()-4);
    }

}