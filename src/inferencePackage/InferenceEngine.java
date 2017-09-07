package inferencePackage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import factValuePackage.*;
import nodePackage.*;


public class InferenceEngine {
	private NodeSet nodeSet;
    private AssessmentState ast;
    private List<Node> nodeFactList;
    private ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("nashorn");
 
    
    
   
//    private int ruleIndex = 0;

    public InferenceEngine(NodeSet nodeSet)
    {
    	this.nodeSet = nodeSet;
    	ast = newAssessmentState();
    	
    	nodeFactList = new ArrayList<>(nodeSet.getNodeSortedList().size()*2); // contains all rules set as a fact given by a user from a ruleList
    	
    }

    public void addNodeSet(NodeSet nodeSet2)
    {
    	
    }
    
    public NodeSet getNodeSet()
    {
    	return this.nodeSet;
    }
    public AssessmentState getAssessmentState()
    {
    	return this.ast;
    }
    
    public AssessmentState newAssessmentState()
    {
    	int initialSize = nodeSet.getNodeSortedList().size() * 2;
    	AssessmentState ast = new AssessmentState();
    	List<String> inclusiveList = new ArrayList<>(initialSize);
    	List<String> exclusiveList = new ArrayList<>(initialSize);
    	List<Node> summaryList = new ArrayList<>(initialSize);
    	ast.setInclusiveList(inclusiveList);
    	ast.setExclusiveList(exclusiveList);
    	ast.setSummaryList(summaryList);
    	
    	return ast;
    	
    }
    
    
    
    /*
     * this method is to extract all variableName of Nodes, and put them into a List<String>
     * it may be useful to display and ask a user to select which information they do have even before starting Inference process
     */
    public List<String> getListOfVariableNameOfNodes()
    {
    	List<String> variableNameList = null;
    	nodeSet.getNodeMap().values().stream().forEachOrdered(node -> variableNameList.add(node.getVariableName()));
    	
    	return variableNameList;
    }
    /*
     * this method allows to store all information via GUI even before starting Inference process. 
     */
    public void addNodeFact(String nodeVariableName, FactValue fv)
    {
    	nodeSet.getNodeMap().values().stream()
    								 .forEachOrdered((node) -> {
    									 if(node.getVariableName().equals(nodeVariableName))
										 {
    										 nodeFactList.add(node);
										 }
    								});  
		ast.getWorkingMemory().put(nodeVariableName, fv);

    }
    
    /*
     * this method is to find all relevant Nodes(immediate child nodes of the most parent) with given information from a user
     * while finding out all relevant factors, all given information will be stored in AssessmentState.workingMemory
     */
    public List<Node> findRelevantFactors()
    {
    	List<Node> relevantFactorList = new ArrayList<>();
    	if(!nodeFactList.isEmpty())
    	{
    		nodeFactList.stream().forEachOrdered(node -> {
    			if(!nodeSet.getDependencyMatrix().getInDependencyList(node.getNodeId()).isEmpty())
    			{
    				Node relevantNode = auxFindRelevantFactors(node);
    				relevantFactorList.add(relevantNode);
    			}
    		});
    	}
    	return relevantFactorList;
    }
    public Node auxFindRelevantFactors(Node node)
    {
    	Node relevantFactorNode = null;
    	List<Integer> incomingDependencyList = nodeSet.getDependencyMatrix().getInDependencyList(node.getNodeId()); // it contains all id of parent node where dependency come from
    	if(!incomingDependencyList.isEmpty())
    	{
    		for(int i = 0; i < incomingDependencyList.size(); i++)
    		{
    			Node parentNode = nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(incomingDependencyList.get(i)));
    			if(!nodeSet.getDependencyMatrix().getInDependencyList(parentNode.getNodeId()).isEmpty() 
    					&& !parentNode.getNodeName().equals(nodeSet.getNodeSortedList().get(0).getNodeName()))
    			{
    				relevantFactorNode = auxFindRelevantFactors(parentNode);

    			}
    		}
    		
    	}
    	return relevantFactorNode;
    }
    
    /*
     * this method uses backward-chaining, and it will return node to be asked of a given assessment, which has not been determined and 
     * does not have any child nodes if the goal node of the given assessment has still not been determined.
     */
    public Node getNextQuestion(Assessment ass)
    {
	    	if(!ast.getInclusiveList().contains(ass.getGoalNode().getNodeName()))
		{
	    		ast.getInclusiveList().add(ass.getGoalNode().getNodeName());
		}
    	
	    	/*
	    	 * Default goal rule of a rule set which is a parameter of InferenceEngine will be evaluated by forwardChaining when any rule is evaluated within the rule set
	    	 */
	    	if(ast.getWorkingMemory().get(ass.getGoalNode().getNodeName())== null)
	    	{
	    		for (int i = ass.getGoalNodeIndex(); i < nodeSet.getNodeSortedList().size(); i++)
	  	       {
	    				Node node = nodeSet.getNodeSortedList().get(i);
	  	            
	  	
	  	            /*
	  	             * Step1. does the rule currently being been checked have child rules && not yet evaluated && is in the inclusiveList?
	  	             *     if no then ask a user to evaluate the rule, 
	  	             *                and do back propagating with a result of the evaluation (note that this part will be handled in feedAnswer())
	  	             *     if yes then move on to following step
	  	             *     
	  	             * Step2. does the rule currently being been checked have child rules? 
	  	             *     if yes then add the child rules into the inclusiveList
	  	             */
	  	            if (!hasChildren(node) && ast.getInclusiveList().contains(node.getNodeName()) && !canEvaluate(node))
	  	            {
		  	            	ass.setNodeToBeAsked(node);
		  	            	int indexOfRuleToBeAsked = i;
		  	            	System.out.println("indexOfRuleToBeAsked : "+indexOfRuleToBeAsked);
		  	            	return ass.getNodeToBeAsked();
	  	            }
		            else
		            {
		            	
			            	if(hasChildren(node) && !ast.getWorkingMemory().containsKey(node.getVariableName()) 
			            			&& !ast.getWorkingMemory().containsKey(node.getNodeName()) && ast.getInclusiveList().contains(node.getNodeName()))
			            	{
			            		addChildRuleIntoInclusiveList(node);
			            	}
		            }
	  	       }
	    	} 	
	    	return ass.getNodeToBeAsked();
    }
    
    public List<String> getQuestionsfromNodeToBeAsked(Node nodeToBeAsked)
    {
    	List<String> questionList = new ArrayList<>();
    	
    	boolean isVariableInFactMap = nodeSet.getInputMap().get(nodeToBeAsked.getVariableName()) != null? true:false;
    	boolean isValueInFactMap = nodeSet.getInputMap().get(nodeToBeAsked.getFactValue().getValue().toString()) != null? true:false;
    	if(isVariableInFactMap)
    	{
    		questionList.add(nodeToBeAsked.getVariableName());
    	}
    	
    	if(isValueInFactMap)
    	{
    		questionList.add(nodeToBeAsked.getFactValue().getValue().toString());
    	}
    	
    	return questionList;
    }
    
    public FactValueType findTypeOfElementToBeAsked(Node node)
    {
    	/*
    	 * FactValueType can be handled as of 16/06/2017 is as follows;
    	 *  1. TEXT, STRING;
    	 *  2. INTEGER, NUMBER;
    	 *  3. DOUBLE, DECIMAL;
    	 *  4. BOOLEAN;
    	 *  5. DATE;
    	 *  6. HASH;
    	 *  7. UUID; and
    	 *  8. URL.   
    	 * rest of them (LIST, RULE, RULE_SET, OBJECT, UNKNOWN, NULL) can't be handled
    	 */
    	FactValueType fvt = null;
    	
    	String nodeVariableName = node.getVariableName();
    	String nodeValue = node.getFactValue().getValue().toString();
    	FactValue factValueForNodeVariable = this.nodeSet.getFactMap().get(nodeVariableName) == null? this.nodeSet.getInputMap().get(nodeVariableName):this.nodeSet.getFactMap().get(nodeVariableName);
    	FactValueType factValueTypeForNodeVariable = factValueForNodeVariable != null? factValueForNodeVariable.getType():null;
    	FactValue factValueForNodeValue = this.nodeSet.getFactMap().get(nodeValue) == null? this.nodeSet.getInputMap().get(nodeValue):this.nodeSet.getFactMap().get(nodeValue);
    	FactValueType factValueTypeForNodeValue = factValueForNodeValue != null? factValueForNodeValue.getType():null;
    	if((factValueTypeForNodeVariable != null && factValueTypeForNodeVariable.equals(FactValueType.BOOLEAN)) || (factValueTypeForNodeValue != null && factValueTypeForNodeValue.equals(FactValueType.BOOLEAN)))
    	{
    		fvt = FactValueType.BOOLEAN;
    	}
    	else if((factValueTypeForNodeVariable != null && factValueTypeForNodeVariable.equals(FactValueType.DATE)) || (factValueTypeForNodeValue != null && factValueTypeForNodeValue.equals(FactValueType.DATE)))
    	{
    		fvt = FactValueType.DATE;
    	}
    	else if((factValueTypeForNodeVariable != null && (factValueTypeForNodeVariable.equals(FactValueType.DECIMAL) || factValueTypeForNodeVariable.equals(FactValueType.DOUBLE))) || (factValueTypeForNodeValue != null && (factValueTypeForNodeValue.equals(FactValueType.DECIMAL) || factValueTypeForNodeValue.equals(FactValueType.DOUBLE))))
    	{
    		fvt = FactValueType.DOUBLE;
    	}
    	else if((factValueTypeForNodeVariable != null && factValueTypeForNodeVariable.equals(FactValueType.HASH)) || (factValueTypeForNodeValue != null && factValueTypeForNodeValue.equals(FactValueType.HASH)))
    	{
    		fvt = FactValueType.HASH;
    	}
    	else if((factValueTypeForNodeVariable != null && factValueTypeForNodeVariable.equals(FactValueType.URL)) || (factValueTypeForNodeValue != null && factValueTypeForNodeValue.equals(FactValueType.URL)))
    	{
    		fvt = FactValueType.URL;
    	}
    	else if((factValueTypeForNodeVariable != null && factValueTypeForNodeVariable.equals(FactValueType.UUID)) || (factValueTypeForNodeValue != null && factValueTypeForNodeValue.equals(FactValueType.UUID)))
    	{
    		fvt = FactValueType.UUID;
    	}
    	else if((factValueTypeForNodeVariable != null && (factValueTypeForNodeVariable.equals(FactValueType.INTEGER) || factValueTypeForNodeVariable.equals(FactValueType.NUMBER))) || (factValueTypeForNodeValue != null && (factValueTypeForNodeValue.equals(FactValueType.INTEGER) || factValueTypeForNodeValue.equals(FactValueType.NUMBER))))
    	{
    		fvt = FactValueType.INTEGER;
    	}
    	else if((factValueTypeForNodeVariable != null && (factValueTypeForNodeVariable.equals(FactValueType.STRING)|| factValueTypeForNodeVariable.equals(FactValueType.TEXT))) || (factValueTypeForNodeValue != null && (factValueTypeForNodeValue.equals(FactValueType.STRING) || factValueTypeForNodeValue.equals(FactValueType.TEXT))))
    	{
    		fvt = FactValueType.STRING;
    	}
    	
    	return fvt;
    }
    /*
     * this is to check whether or not a node can be evaluated with all information in the workingMemory. If there is information for a value of node's value(FactValue), then the node can be evaluated otherwise not.
     * In order to do it, AssessmentState.workingMemory must contain a value for variable of the rule, 
     * and rule type must be either COMPARISON, ITERATE or VALUE_CONCLUSION because they are the ones only can be the most child nodes.		
     */
    public boolean canEvaluate(Node node)
    {
    	
    	boolean canEvaluate = false;
    	LineType lineType = node.getLineType();
    	/*
    	 * the reason for checking only VALUE_CONCLUSION, COMPARISON and ITERATE type of node is that they are the only ones can be the most child nodes in rule structure.
    	 * other type of node must be a parent of other types of node.
    	 * In addition, the reason being to check only if there is a value for a variableName of the node in the workingMemory is that
    	 * only a value for variableName of the node is needed to evaluate the node. even if the node is ValueConclusionLine, it wouldn't be matter because
    	 * variableName and nodeName will have a same value if the node is the most child node, which means that the statement for the node does NOT contain
    	 * 'IS' keyword. 
    	 */
    	if(((lineType.equals(LineType.VALUE_CONCLUSION) || lineType.equals(LineType.COMPARISON)) && ast.getWorkingMemory().containsKey(node.getVariableName()))
    			||(lineType.equals(LineType.ITERATE) && ast.getWorkingMemory().containsKey(node.getNodeName())))
    	{
    		canEvaluate = true;
    		/*
    		 * the reason why ast.setFact() is used here rather than this.feedAndwerToNode() is that LineType is already known, and target node object is already found. 
    		 * node.selfEvaluation() returns a value of the node's self-evaluation hence, node.getNodeName() is used to store a value for the node itself into a workingMemory
    		 */
    		ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, this.nodeSet.getDependencyMatrix().getDependencyType(node.getNodeId(), node.getNodeId())));
    	}
    	
    	
    	return canEvaluate;
    }

    /* 
     * this method is to add fact or set a node as a fact by using AssessmentState.setFact() method. it also is used to feed an answer to a being asked node.
     * once a fact is added then forward-chain is used to update all effected nodes' state, and workingMemory in AssessmentState class will be updated accordingly
     * the reason for taking nodeName instead nodeVariableName is that it will be easier to find an exact node with nodeName
     * rather than nodeVariableName because a certain nodeVariableName could be found in several nodes
     */
    public <T> void feedAnswerToNode(String NodeName, String questionName, T nodeValue)
    {
    	Node targetNode = nodeSet.getNodeMap().get(NodeName);
//    	LineType targetNodeType = targetNode.getLineType();
    	
    	FactValueType targetNodeValueType = targetNode.getFactValue().getType();
    	FactValue fv = null;
    	if(targetNodeValueType.equals(FactValueType.BOOLEAN))
    	{
    		fv = FactValue.parse(Boolean.parseBoolean((String)nodeValue));
    	}
    	else if(targetNodeValueType.equals(FactValueType.DATE))
    	{
//    		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    		String[] dateArray = ((String)nodeValue).split("/");
    		LocalDate factValueInDate = LocalDate.of(Integer.parseInt(dateArray[2]), Integer.parseInt(dateArray[1]), Integer.parseInt(dateArray[0]));
//    		LocalDate factValueInDate = LocalDate.parse((String)nodeValue, formatter);
    		
    		fv = FactValue.parse(factValueInDate);
    	}
    	else if(targetNodeValueType.equals(FactValueType.DOUBLE))
    	{
    		fv = FactValue.parse(Double.parseDouble((String)nodeValue));
    	}
    	else if(targetNodeValueType.equals(FactValueType.INTEGER))
    	{
    		fv = FactValue.parse(Integer.parseInt((String)nodeValue));
    	}
    	else if(targetNodeValueType.equals(FactValueType.LIST))
    	{
    		fv = FactValue.parse((List<FactValue>)nodeValue);
    	}
    	else if(targetNodeValueType.equals(FactValueType.STRING))
        {
    		fv = FactValue.parse((String) nodeValue);
        }
    	else if(targetNodeValueType.equals(FactValueType.HASH))
    	{
    		fv = FactValue.parseHash((String)nodeValue);
    	}
    	else if(targetNodeValueType.equals(FactValueType.URL))
    	{
    		fv = FactValue.parseURL((String)nodeValue);
    	}
    	else if(targetNodeValueType.equals(FactValueType.UUID))
    	{
    		fv = FactValue.parseUUID((String)nodeValue);
    	}
    	
    	
    	/*
    	 * once we got an answer from a user we need to do followings;
    	 *  1. set a value of a question, which is a value for a variableName of the node, being asked to a user in a workingMemory
    	 *  2. set a value of the node itself in a workingMemory.
    	 *     
    	 */
    	if(questionName.equals(targetNode.getVariableName()))
    	{
        	ast.setFact(targetNode.getVariableName(), fv);
    	}
    	else if(questionName.equals(targetNode.getFactValue().getValue().toString()))
    	{
        	ast.setFact(targetNode.getFactValue().getValue().toString(), fv);
    	}
    	FactValue selfEvalFactValue = targetNode.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, this.nodeSet.getDependencyMatrix().getDependencyType(targetNode.getNodeId(), targetNode.getNodeId()));
    	
    	if(selfEvalFactValue != null)
    	{
    		ast.setFact(targetNode.getNodeName(), selfEvalFactValue); // add the value of targetNode itself into the workingMemory
        	
        	

        	/*
        	 * Note: in order to get summary view, each rules can be found in summaryList, and 
        	 *       actual evaluation value can be found in workingMemory by looking up with each rules' variable
        	 */
        	ast.getSummaryList().add(targetNode);
        	/*
        	 * once any rules are set as fact and stored into the workingMemory, forward-chaining(back-propagation) needs to be done
        	 */
        	forwardChaining(nodeSet.findNodeIndex(targetNode.getNodeName()));
    	}
        
    	
    	
    }
    
    /*
     * this method still needs to be completed, in particular update parent nodes of the being answered node.
     */
    public void forwardChaining(int nodeIndex)
    {
    	/*
    	 * all nodes prior to 'nodeIndex' in the nodeList(sortedList) of nodeSet should be looked at to be updated once the node at a nodeIndex is being answered for following reasons;
    	 * 1. regardless the nodeList is sorted with Khan's algorithm which is based on BFS, 
    	 *    all nodes in the inclusiveList and prior to the node at a nodeIndex are possibly parent nodes of the node at nodeIndex; 
    	 * 2. the list may be sorted based on historical statistic record, and if it is the case then the sorting is based on Deepening and Greedy algorithm;
    	 * 3. there could be a node sharing parents nodes or children nodes in the list
    	 * And therefore, updating all nodes in the list based on a given fact is a safe way to get a next question and complete assessment.
    	 */
    	
    	IntStream.range(0, nodeIndex+1).forEach(i -> {
    		
    		Node node = nodeSet.getNodeSortedList().get(nodeIndex-i);
    		
    		/* updating all nodes' state prior to ruleIndex in the ruleList 
    		 * by setting the value value of nodeVariables and nodeName of each node in workignMemory
    		 */
 
    		
    		/*
             *if the node currently being checked exists in the 'inclusiveList'
             *then check if the node has any children then update the current node's state based on the children's state
             *Note: a question will be asked if only if the being asked question is in the inclusiveLsit, however, following condition
             *is to do double check if the being asked question is in the inclusiveList
             */
    		if(ast.getInclusiveList().contains(node.getNodeName()))
    		{
    			
    			backPropagation(nodeIndex-i);
    		}
    		
    		 /*
             *following 'if' condition is to do
             *adding parent nodes to 'inclusiveList' if only the current node is in the 'workingMemory' list 
             *because only parent nodes of the node that is in 'workingMemory' list are only relevant.
             *this condition helps to do faster performance
             */
    		
            if(ast.getWorkingMemory().containsKey(node.getVariableName()) || ast.getWorkingMemory().containsKey(node.getNodeName()))
            {
            	/*
            	 * background of following method is as listed below
            	 * 1. adding child rules into inclusiveList sometimes miss out relevant rules because some rules have more than two parent hence tracking only child rules
            	 * would not be enough to find all relevant rules. As result, finding child rule of a certain rule and parent rule of the child rule will cover all relevant rules for an assessment
            	 */
            	addParentIntoInclusiveList(node); // adding all parents rules into the 'inclusiveList' if there is any
            }
    		
    	});
    	
        
        /*
        following if-else statement is only to mimic a system function
        checking 'workingMemory' is empty or not should be done at the beginning of this 'forwardChaining' process
        */
//        if(ast.getWorkingMemory().isEmpty())
//        {
//            System.out.println("none of rules are true");
//        }
//        else
//        {
//            for(Object rule: ast.getWorkingMemory().keySet())
//            {
//            	FactValueType ruleValueType = ast.getWorkingMemory().get(rule).getType();
//            	
//            	if(ruleValueType.equals(FactValueType.BOOLEAN))
//            	{
//            		System.out.println(((FactStringValue)rule).getValue() +" : "+ ((FactBooleanValue)ast.getWorkingMemory().get(rule)).getValue());
//            	}
//            	else if(ruleValueType.equals(FactValueType.DATE))
//            	{
//            		System.out.println(((FactStringValue)rule).getValue() +" : "+ ((FactDateValue)ast.getWorkingMemory().get(rule)).getValue());
//            	}
//            	else if(ruleValueType.equals(FactValueType.DOUBLE))
//            	{
//            		System.out.println(((FactStringValue)rule).getValue() +" : "+ ((FactDoubleValue)ast.getWorkingMemory().get(rule)).getValue());
//            	}
//            	else if(ruleValueType.equals(FactValueType.INTEGER))
//            	{
//            		System.out.println(((FactStringValue)rule).getValue() +" : "+ ((FactIntegerValue)ast.getWorkingMemory().get(rule)).getValue());
//            	}
//            }
//        }
    	
    }
    
    /*
     *this method is to find all parent rules of a given rule, and add them into the ' inclusiveList' for future reference
     */
    public void addParentIntoInclusiveList(Node node)
    {
    	List<Integer> nodeInDependencyList = nodeSet.getDependencyMatrix().getInDependencyList(node.getNodeId());
        if(!nodeInDependencyList.isEmpty()) // if rule has parents
        {
        	nodeInDependencyList.stream().forEachOrdered(i -> {
        		Node parentNode = nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i));
        		if(!ast.getInclusiveList().contains(parentNode.getNodeName()))
        		{
        			ast.getInclusiveList().add(parentNode.getNodeName());
        		}
        	});
          
        }
    }
    
/*
 * once a user feed an answer to the engine, the engine will propagate the entire RuleSet or Assessment base on the answer
 * during the back-propagation, the engine checks if the current rule the engine is checking;
 * 1. has been determined;
 * 2. has any child rules;
 * 3. can be determined on ground of various condition.
 * 
 *  once the current checking rule meets the condition then set the state of the rule as DETERMINED, and add it to summaryList for summary view.
 * 
 * TODO need to consider ITERATE line type for this back-propagation due to there would be possibilities a list for the value of ITERATE will be generated or provided
 * during other rules back-propagation
 */
    public void backPropagation(int i) 
    {
    	
    	Node currentNode = nodeSet.getNodeSortedList().get(i);
    	LineType currentLineType = currentNode.getLineType();
    	/*
         *following 'if' statement is to double check if the rule has any children or not.
         *it will be already determined by asking a question to a user if it doesn't have any children .
         */
       if (!ast.getWorkingMemory().containsKey(currentNode.getVariableName()) 
    		   && hasChildren(currentNode) && canDetermine(currentNode, currentLineType) )
       {
//    	   currentRule.setState(RuleState.DETERMINED);
    	   ast.getSummaryList().add(currentNode); // add currentRule into SummeryList as the rule determined
       }
    }
    
    public boolean canDetermine(Node node, LineType lineType)
    {
    	boolean canDetermine = false;
    	/*
    	 * Any type of node/line can have either 'OR' or 'AND' child nodes
    	 * do following logic to check whether or not the node is determinable
    	 * 1. check the node/line type
    	 * 2. within the node/line type, check if it has 'OR' child nodes or 'AND' child nodes ( nodeSet.getDependencyMatrix.gete.getOrOutDependency().isEmpty() or rule.getAndOutDependency().isEmpty())    
    	 * 
    	 * -----ValueConclusion Type
    	 * there will be two cases for this type
    	 *    V.1 'TRUE' or "FALSE' value outcome case
    	 *    	   V.1.1 if it has 'OR' child nodes
    	 *    			 V.1.1.1 TRUE case
    	 *    					 if there is any of child node is 'true'
    	 *    					 then trim off 'UNDETERMINED' child nodes, which are not in 'workingMemory', other than 'MANDATORY' child nodes
    	 *    			 V.1.1.2 FALSE case
    	 *    					 if its all 'OR' child nodes are determined and all of them are 'false'
    	 *    	   V.1.2 if it has 'AND' child nodes
    	 *       		 V.1.2.1 TRUE case
    	 *       				 if its all 'AND' child nodes are determined and all of them are 'true'
    	 *       		 V.1.2.2 FALSE case
    	 *       				 if its all 'AND' child nodes are determined and all of them are 'false'
    	 *                     	 , and there is no need to trim off 'UNDETERMINED' child nodes other than 'MANDATORY' child nodes
    	 *                       because since 'virtual node' is introduced, any parent nodes won't have 'OR' and 'AND' dependency at the same time
    	 *              
    	 *         V.1.3 other than above scenario it can't be determined in 'V.1' case
    	 *    
    	 *    V.2 a case of that the value in the rule text can be used as a value of its rule's variable
    	 *    	   V.2.1 if it has 'OR' child nodes
    	 *    			 V.2.1.1 the value CAN BE USED case
    	 *    					 if its any of child node is 'true'
    	 *    					 then trim off 'UNDETERMINED' child nodes, which are not in 'workingMemory', other than 'MANDATORY' child nodes
    	 *    			 V.2.1.2 the value CANNOT BE USED case
    	 *    					 if its all 'OR' child nodes are determined and all of them are 'false'
    	 *    	   V.2.2 if it has 'AND' child nodes
    	 *    			 V.2.2.1 the value CAN BE USED case
    	 *    					 if its all 'AND' child nodes are determined and all of them are 'true'
    	 *    			 V.2.2.2 the value CANNOT BE USED case
    	 *    					 if its all 'AND' child nodes are determined and all of them are 'false'
    	 *                     	 , and there is no need to trim off 'UNDETERMINED' child nodes other than 'MANDATORY' child nodes
    	 *                       because since 'virtual node' is introduced, any parent nodes won't have 'OR' and 'AND' dependency at the same time
    	 *         
    	 *         V.2.3 other than above scenario it can't be determined in 'V.1' case
    	 *              
    	 * -----ExprConclusion Type
    	 *    within rule text file, this node has two types of child node, 'NEEDS' and 'WANTS'.
    	 *    'NEEDS' child rule will be translated as 'MANDATORY_AND', and 'WANTS' child node will be 'OR' in the rule structure.
    	 *    In addition, back-propagation(evaluation) part will be done within the rule itself,
    	 *    Due to the case of this node type having 'NEEDS' and 'WANTS' child nodes at the same time, 
    	 *    the node type can have either only 'MANDATORY_AND's, or 'MANDATORY_AND' and 'OR' child rules
    	 *    As a result, followings needs checking
    	 *    E.1. if it has 'OR' child nodes
    	 *         E.1.1 the node CAN BE EVALUATED case
    	 *               if 'MANDATORY_OR' child node is determined, which means 'MANDATORY_AND' child node is determined
    	 *               then trim off 'UNDETERMINED' child nodes
    	 *         E.1.2 the rule CAN'T BE EVALUATED case
    	 *               if 'MANDATORY_OR' child node is not determined yet, which means 'MANDATORY_AND' child is not determined yet.
    	 *               
    	 *    E.2 if it has 'AND' child rules
    	 *        E.2.1 the rule CAN BE EVALUATED case
    	 *              if all 'MANDATORY_AND' rules are determined.
    	 * 
    	 * -----Comparison Type
    	 *    within rule text file, this node must not have any child nodes. 
    	 *    However, in the rule structure of NodeSet class, the rule which contains '=' operator' can have only 'OR' child node of ValueConclusionLine Type or ExprConclusionLine Type if there is a corresponded node(s).
    	 *    In addition, the value of node type must exist among its child nodes' value.
    	 *    As a result, in order to confirm that whether or not the node type can be determined, there must be a value of its variableName in the workingMemory. 
    	 *    Back-propagation(evaluation) part will be done within the node itself by executing selfEvaluate().
    	 *
    	 *    
    	 * Note: the reason why only ResultType and ExpressionType are evaluated with selfEvaluation() is as follows;
    	 *       1. ResultType is only evaluated by comparing a value of rule's variable in workingMemory with the value in the rule
    	 *       2. ExpressionType is only evaluated by retrieving a value(s) of needed child rule(s)   
    	 *       3. BooleanConclusionType and ValueConclusionType is evaluated under same combination of various condition, and trimming dependency is involved.
    	 *          As a result, if selfEvaluate() is used in both rule type then each rule will have source code. 
    	 *        
    	 *       
    	 */
    	List<Integer> nodeOrOutDependencies = nodeSet.getDependencyMatrix().getOROutDependencyList(node.getNodeId());
    	List<Integer> nodeAndOutDependencies = nodeSet.getDependencyMatrix().getAndOutDependencyList(node.getNodeId());
    	int nodeOption = this.getNodeSet().getDependencyMatrix().getDependencyType(node.getNodeId(), node.getNodeId());
    	
    	if(LineType.VALUE_CONCLUSION.equals(lineType))
    	{
    		/*
    		 *	1. the rule is a plain statement
    		 *		- evaluate based on outcome of its child nodes
    		 *		  there only will be an outcome of entire rule statement with negation or known type value
    		 *		  , which should be handled within selfEvaluate()
    		 *	2. the rule is a statement of 'A IS B'
    		 *		- evaluate based on outcomes of its child nodes
    		 *		  there will be an outcome for a statement of that is it true for 'A = B'?
    		 *		  , and out
    		 * 	3. the rule is a statement of 'A IS IN LIST: B'
    		 * 	4. the rule is a statement of 'needs(wants) A'. this is from a child node of ExprConclusionLine type 
    		 */
    		boolean isInStatement = ((ValueConclusionLine)node).getIsInStatementFormat();
    		
    		/*
    		 * isAnyOrDependencyTrue() method contains trimming off method to cut off any 'UNDETERMINED' state 'OR' rules. 
    		 */
    		if(nodeAndOutDependencies.isEmpty() && !nodeOrOutDependencies.isEmpty()) // rule has only 'OR' child rules 
    		{
    			
    			if(isAnyOrDependencyTrue(node, nodeOrOutDependencies)) //TRUE case
    			{
    				canDetermine = true;
    				if(isInStatement)
					{
    					ast.setFact(node.getVariableName(), FactValue.parse(true));
					}
    				else
    				{
    					ast.setFact(node.getVariableName(), node.getFactValue());
    				}
    				
    				
    				ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption));
    			}
    			else if(isAllOrDependencyDetermined(nodeOrOutDependencies) && !isAnyOrDependencyTrue(node, nodeOrOutDependencies)) //FALSE case
    			{
    				canDetermine = true;
    				if(isInStatement)
					{
    					ast.setFact(node.getVariableName(), FactValue.parse(false));
					}
    				else
    				{
    					ast.setFact(node.getVariableName(), FactValue.parse(node.getVariableName()));
    				}
    				    				
    				ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption));

    			}
    		}
    		else if(!nodeAndOutDependencies.isEmpty() && nodeOrOutDependencies.isEmpty())// rule has only 'AND' child rules
    		{
    			if(isAllAndDependencyDetermined(nodeAndOutDependencies) && isAllAndDependencyTrue(node, nodeAndOutDependencies)) // TRUE case
				{
    				canDetermine = true;
    				if(isInStatement)
					{
    					ast.setFact(node.getVariableName(), FactValue.parse(false));
					}
    				else
    				{
    					ast.setFact(node.getVariableName(), FactValue.parse(node.getVariableName()));
    				}
    				    				
    				ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption));

				}
    			/*
    			 * 'isAnyAndDependencyFalse()' contains a trimming off dependency method 
    			 * due to the fact that all undetermined 'AND' rules need to be trimmed off when any 'AND' rule is evaluated as 'NO'
               	 * , which does not influence on determining a parent rule's evaluation.
               	 * 
    			 */
    			else if(isAllAndDependencyDetermined(nodeAndOutDependencies) && isAnyAndDependencyFalse(nodeAndOutDependencies)) //FALSE case
    			{
    				canDetermine = true;
    				if(isInStatement)
					{
    					ast.setFact(node.getVariableName(), FactValue.parse(false));
					}
    				else
    				{
    					ast.setFact(node.getVariableName(), FactValue.parse(node.getVariableName()));
    				}
    				
    				ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption));
    				
    			}
		
    		}
    	}
    	else if(LineType.EXPR_CONCLUSION.equals(lineType))
    	{

    		if(!nodeAndOutDependencies.isEmpty() && nodeOrOutDependencies.isEmpty()) // rule has 'MANDATORY_OR' and 'OR' child rules 
    		{
    			for(int i=0; i < nodeOrOutDependencies.size(); i++)
    			{
    				int mandatoryOrDependencyType = DependencyType.getMandatory() | DependencyType.getOr();
    				if((nodeSet.getDependencyMatrix().getDependencyMatrixArray()[node.getNodeId()][nodeOrOutDependencies.get(i)] & mandatoryOrDependencyType) == mandatoryOrDependencyType)
    				{
    					if(ast.getWorkingMemory().get(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(nodeOrOutDependencies.get(i))).getVariableName()) != null)
    					{
    						canDetermine = true;
    						ast.setFact(node.getVariableName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption)); // add currentRule into the workingMemory
    					}
    				}
    				else
    				{
    					ast.getInclusiveList().remove(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i)).getNodeName());
    				}
    			}    			
    		}
    		else if(nodeAndOutDependencies.isEmpty() &&!nodeOrOutDependencies.isEmpty())// rule has only 'MANDATORY_AND' child rules
    		{
    			if(allNeedsChildDetermined(node, nodeAndOutDependencies)) // TRUE case
				{
    				canDetermine = true;
    				/*  
 	                `* The reason why ast.setFact() is used here rather than this.addFactToRule() is that ruleType is already known, and target rule object is already found. 
 	                 */
 	                ast.setFact(node.getVariableName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption)); // add currentRule into the workingMemory
				}
		
    		}
    	}
    	else if(LineType.COMPARISON.equals(lineType))
    	{
    		
    		if(ast.getWorkingMemory().get(node.getVariableName()) != null)
    		{
    			canDetermine = true;
    			/*  
    			 * The reason why ast.setFact() is used here rather than this.addFactToRule() is that ruleType is already known, and target rule object is already found. 
    			 */
     		   ast.setFact(node.getNodeName(), node.selfEvaluate(ast.getWorkingMemory(), this.scriptEngine, nodeOption)); // add currentRule into the workingMemory
    		}
    	}
    	
    	return canDetermine;
    }
    

   
    
    public String getDefaultGoalRuleQuestion() 
    {
    	return nodeSet.getDefaultGoalNode().getNodeName();
    }
    public String getAssessmentGoalRuleQuestion(Assessment ass)
    {
    	return ass.getGoalNode().getNodeName();
    }
    
    public FactValue getDefaultGoalRuleAnswer() {
    	return ast.getWorkingMemory().get(nodeSet.getDefaultGoalNode().getVariableName());
    }
    public FactValue getAssessmentGoalRuleAnswer(Assessment ass)
    {
    	return ast.getWorkingMemory().get(ass.getGoalNode().getVariableName());
    }


    /*
     Returns boolean value that can determine whether or not the given rule has any children
     this method is used within the process of backward chaining.
     */
    public boolean hasChildren(Node node)
    {
        boolean hasChildren = false;
        if (!nodeSet.getDependencyMatrix().getOutDependencyList(node.getNodeId()).isEmpty())
        {
            hasChildren = true;
        }
        return hasChildren;
    }

    /*
    the method adds all children rules of relevant parent rule into the 'inlcusiveList' if they are not in the list.
    */
    public void addChildRuleIntoInclusiveList(Node node)
    {
    	List<Integer> childrenListOfNode = nodeSet.getDependencyMatrix().getOutDependencyList(node.getNodeId());
    	childrenListOfNode.stream().forEachOrdered(item -> {
    		String childNodeName = nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(item)).getNodeName();
    		if(!ast.getInclusiveList().contains(childNodeName))
    		{
    			ast.getInclusiveList().add(childNodeName);
    		}
    	});
        
    }


    public boolean isAnyOrDependencyTrue(Node node, List<Integer> orOutDependencies)
    {
        boolean isAnyOrDependencyTrue = false;
        if (!orOutDependencies.isEmpty())
        {
        	List<Integer> trueOrOutNodesList = orOutDependencies.stream().filter(i -> 
        	ast.getWorkingMemory().get(nodeSet.getNodeIdMap().get(i)) != null && ast.getWorkingMemory().get(nodeSet.getNodeIdMap().get(i)).getValue().equals(true))
        																 .collect(Collectors.toList());
        	
        	if(!trueOrOutNodesList.isEmpty())
        	{
        		isAnyOrDependencyTrue = true;
        		orOutDependencies.stream().forEachOrdered(i -> {
        			trueOrOutNodesList.stream().forEachOrdered(n -> {
        				if(i != n)
        				{
        					trimDependency(node, nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i)));
        				}
        			});
        		});
        	}
        }
//        else if(orOutDependencies == null)
//        {
//    		isAnyOrDependencyTrue = true;
//        }
        return isAnyOrDependencyTrue;
    }

    public void trimDependency(Node parentNode, Node childNode)
    {
    	int dpType = nodeSet.getDependencyMatrix().getDependencyMatrixArray()[parentNode.getNodeId()][childNode.getNodeId()];
    	int mandatoryDependencyType = DependencyType.getMandatory();
    	if((dpType & mandatoryDependencyType) != mandatoryDependencyType)
    	{
    		ast.getInclusiveList().remove(childNode.getNodeName());
    	}
    }
    
    public boolean isAnyAndDependencyFalse(List<Integer> andOutDependencies)
    {
        boolean isAnyAndDependencyFalse = false;
        
        List<Integer> falseAndList = andOutDependencies.stream().filter(i -> ast.getWorkingMemory().get(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i))).getValue().toString().equals("false")).collect(Collectors.toList());

        if(falseAndList.size() > 0)
        {
        	isAnyAndDependencyFalse = true;
        	andOutDependencies.stream().forEachOrdered(i -> {
        		falseAndList.stream().forEachOrdered(f -> {
        			if(i != f)
        			{
        				ast.getInclusiveList().remove(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i)).getNodeName());
        			}
        		});
        	});
        }
        else if(andOutDependencies.size() == 0)
        {
        	isAnyAndDependencyFalse = true;
        }
        return isAnyAndDependencyFalse;
    }
    
    
    public boolean isAllAndDependencyTrue(Node parentRule, List<Integer> andOutDependencies)
    {
        boolean isAllAndTrue = false;

        List<Integer> determinedTrueAndOutDependencies = andOutDependencies.stream().filter(i ->
    	ast.getWorkingMemory().get(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i))).getValue().toString().equals("true")).collect(Collectors.toList());
        
        
        if(andOutDependencies != null && determinedTrueAndOutDependencies.size() == andOutDependencies.size())
        {
        	isAllAndTrue = true;
        } 
//        else if(andOutDependencies == null) 
//        {
//        	isAllAndTrue = true;
//		}
               return isAllAndTrue;
    }

    public boolean isAllAndDependencyDetermined(List<Integer> andOutDependencies)
    {
        boolean isAllAndDependencyDetermined = false;
        
        List<Integer> determinedAndOutDependencies = andOutDependencies.stream().filter(i ->
    	ast.getWorkingMemory().get(nodeSet.getNodeIdMap().get(i)) != null).collect(Collectors.toList());
        
        
        if(andOutDependencies != null && determinedAndOutDependencies.size() == andOutDependencies.size())
        {
        	isAllAndDependencyDetermined = true;
        }
//        else 
//        {
//        	isAllAndDependencyDetermined = true;
//        }

        return isAllAndDependencyDetermined;
    }

    public boolean isAllOrDependencyDetermined(List<Integer> orOutDependencies)
    {
        boolean isAllOrDependencyDetermined = false;
        
        List<Integer> determinedOrOutDependencies = orOutDependencies.stream().filter(i ->
        	ast.getWorkingMemory().get(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i))) != null
        ).collect(Collectors.toList());
        
        if(orOutDependencies != null && determinedOrOutDependencies.size() == orOutDependencies.size())
        {
        	isAllOrDependencyDetermined = true;
        }
//        else 
//        {
//        	isAllOrDependencyDetermined = true;
//        }

        return isAllOrDependencyDetermined;
    }


    public boolean allNeedsChildDetermined(Node parentNode, List<Integer> outDependency)
    {
    	boolean allNeedsChildDetermined = false;
    	
    	int mandatoryAndDependencyType = DependencyType.getMandatory() | DependencyType.getAnd();
    	List<Integer> determinedList = outDependency.stream().filter(i -> (nodeSet.getDependencyMatrix().getDependencyMatrixArray()[parentNode.getNodeId()][i] & mandatoryAndDependencyType) == mandatoryAndDependencyType
    										&& ast.getWorkingMemory().get(nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i)).getVariableName())!= null).collect(Collectors.toList());
    	
    	if(outDependency.size() == determinedList.size())
    	{
    		allNeedsChildDetermined = true;
    	}    	
    	
    	return allNeedsChildDetermined;
    }
    
//    public void trimOutWantsChild(Node rule)
//    {
//    	List<Dependency> orOutDependency = rule.getOrOutDependency();
//    	Iterator<Dependency> iterator = orOutDependency.iterator();
//    	while(iterator.hasNext())
//    	{
//    		Dependency dp = iterator.next();
//    		Rule childRule = dp.getChildRule();
//    		if(dp.getRelationship().equals(Relationship.OR)&& ast.getInclusiveList().contains(((FactStringValue)childRule.getVariableName()).getValue()))
//    		{
//    			ast.getInclusiveList().remove(childRule.getName().getValue());
//    		}
//    	}
//    }
    /*
     *following trimDependency() is to trim out all UNDETERMINED dependencies once any 'OR' dependency found 'TRUE'
     *except 'OR MANDATORY' dependent rules.
     */
//    public void trimOrDependency(Node parentNode, Node childNode)
//    {
//    	List<Integer> outDependency = nodeSet.getDependencyMatrix().getOutDependencyList(parentNode.getNodeId());
//    	outDependency.stream().forEachOrdered(i -> {
//    		Node tempChildNode = nodeSet.getNodeMap().get(nodeSet.getNodeIdMap().get(i));
//    		
//    		/*
//             * finding child rules of a given rule which meets following criteria;
//             * 1. not having same name as a rule to compare, which is a determined rule
//             * 2. not having dependency with 'MANDATORY_OR' relationship
//             */
//    		
//    		if(tempChildNode.getNodeName().equals(childNode.getNodeName()) && ast.getInclusiveList().contains(childNode.getNodeName()) &&
//    				!nodeSet.getDependencyMatrix().getDependencyMatrixArray()[parentNode.getNodeId()][childNode.getNodeId()].equals(DependencyType.MANDATORY_OR))
//    		{
//    			
//    		}
//    	});
//        List<Dependency> outDependencies = parentRule.getOutDependencies();
//        for (int i = outDependencies.size()-1; i >= 0; i--)
//        {
//            Dependency dp = outDependencies.get(i);
//            Rule tempChildRule = dp.getChildRule();
//            FactValue tempChildVariableName = tempChildRule.getVariableName();
//                       
//            /*
//             * finding child rules of a given rule which meets following criteria;
//             * 1. not having same name as a rule to compare, which is a determined rule
//             * 2. not having dependency with 'MANDATORY_OR' relationship
//             */
//        	if (!tempChildRule.getName().getValue().equals(childRule.getName().getValue()) && 
//    			ast.getInclusiveList().contains(((FactStringValue)childRule.getVariableName()).getValue()) && //child rule's variableName is in the inclusiveList
//        		!ast.getWorkingMemory().containsKey(tempChildVariableName) && // child rule has not been determined
//        		!dp.getRelationship().equals(Relationship.MANDATORY_OR)) // child rule is not 'MANDATORY_OR' dependent
//            {
//              ast.getInclusiveList().remove(tempChildRule.getName().getValue()); // once a rule found meeting the condition, get rid of the rule from the 'inclusiveList'
////                	ast.getExclusiveList().add(tempChildRule.getName().getValue()); 
//            }
//                    
//        }
//
//    }
    
   
     
    /*
    following method is to remove any unknown 'AND' child from 'inclusiveList' when an 'AND' sibling rule is determined.
    once the rule is removed from 'inclusiveList', the rule will not be asked even if it is not determined due to its unnecessary
    */
//    public void trimUnknownAndDependency(Rule parentRule, Rule determinedRuleAsFalse)
//    {
//    	List<Dependency> andOutDependencies = parentRule.getAndOutDependency();
//    	Iterator<Dependency> iterator = andOutDependencies.iterator();
//    	while(iterator.hasNext())
//        {
//        	Dependency dp = iterator.next();
//            Rule childRule = dp.getChildRule();
//            FactValue childVariableName = childRule.getVariableName();
//            String determinedAsFalseRuleName = determinedRuleAsFalse.getName().getValue();
//            	
//        	if(!ast.getWorkingMemory().containsKey(childVariableName) && !childRule.getName().getValue().equals(determinedAsFalseRuleName))
//            {
//            	ast.getInclusiveList().remove(childRule.getName().getValue());
////            	ast.getExclusiveList().add(childRule.getName().getValue()); // this line is to handle more various structure of rule set
//            }     
//            
//        }
//    }
    
 
   
    

    /**
     * make a summary of the assessment rules and answers as a html document
     * using a template for the structure and replacing markers the values
     * @return html
     */
//    public String generateAssessmentSummaryFromTemplate()
//    {    		    	
//     	
//	    Map<String, String> map = new HashMap<>();
//	    
//	    
//	    int i = 0 ; 
//		for (String ruleName : ast.getInclusiveList())
//		{
//			String ruleState = ast.getWorkingMemory().get(ruleName);
//			if (ruleState != null) {
//				map.put("rules["+i+"].number", Integer.toString(i+1));//add question to array
//				map.put("rules["+i+"].question", ruleName);//add question to array
//				map.put("rules["+i+"].answer", ruleState);// add answer to array
//				i++;
//			}
//		}
//		map.put("rules[]", Integer.toString(i)); //specify size of array
//		String html = Document.getTemplate("assessment_summary.xml");
//		html = Document.replaceValuesInHtml(html, map);
//		
//	    return html;
//	}
//
//    
    
    /*
    this method is to reset 'workingMemory' list and 'inclusiveList'
    usage of this method will depend on a user. if a user wants to continue to assessment on a same veteran with same conditions
    then don't need to reset 'workingMemory' and 'inclusiveList' otherwise reset them.
    */
    public void resetWorkingMemoryAndInclusiveList()
    {
        if(!ast.getInclusiveList().isEmpty())
        {
        	ast.getInclusiveList().removeAll(ast.getInclusiveList());
        }
        if(!ast.getWorkingMemory().isEmpty())
        {
        	ast.getWorkingMemory().clear();
        }
    }
    
    
    /*
     * this is to generate Assessment Summary
     * need to modify this method to have correct summary
     */
//    public String generateAssessmentSummary()
//    {    		    	
//    	StringBuilder htmlText = new StringBuilder();
//    	htmlText.append("<!DOCTYPE html>"+"\n"+
//    	                "<html>"+"\n"+
//    	                "<head><title></title></head>"+"\n"+
//    	                "<body><h3> Assessment Summary</h3>"+"\n");
//    	int summaryListSize = ast.getSummaryList().size();
//    	if(summaryListSize != 0 )
//    	{
//    		htmlText.append("<ol type =\"1\">"+"\n");
//    		
//    		for(int i = summaryListSize; i > 0; i--)
//        	{
//        		Rule rule = ast.getSummaryList().get(i);
//        		FactValue factValue = ast.getWorkingMemory().get(rule.getVariableName());
//        		FactValueType factValueType = factValue.getType();
//        		String printingValue = null;
//        		if(factValue != null)
//        		{
//        			if(factValueType.equals(FactValueType.BOOLEAN))
//            		{
//            			printingValue = Boolean.toString(((FactBooleanValue)factValue).getValue());
//            			printingValue = printingValue.toUpperCase();
//            		}
//            		else if(factValueType.equals(FactValueType.DATE))
//            		{
//            			printingValue =((FactDateValue)factValue).getValue().toString();
//            		}
//            		else if(factValueType.equals(FactValueType.DOUBLE))
//            		{
//            			printingValue = Double.toString(((FactDoubleValue)factValue).getValue());
//            		}
//            		else if(factValueType.equals(FactValueType.INTEGER))
//            		{
//            			printingValue = Integer.toString(((FactIntegerValue)factValue).getValue());
//            		}
//            		else if(factValueType.equals(FactValueType.LIST))
//            		{
//            			
//            		}
//        		}
//        		
//        		
//        		
//        		
//        		if(ruleState != null)
//        		{
//        			
//        			htmlText.append("<li>"+ruleName+" : "+ruleState+"</li>"+"\n");
//        		}
//        		
//        	}    
//        	htmlText.append("</ol>"+"\n");
//    	}
//    	htmlText.append("</body>"+"\n"+
//						"</html>"+"\n");
//    	
//     	
//    	return htmlText.toString();
//    }
    
    /*
     * this is to find a condition with a list of given keyword
     */
    public List<String> findCondition(String keyword)
    {
    	int initialSize = nodeSet.getNodeSortedList().size();
    	List<String> conditionList = new ArrayList<>(initialSize);
    	List<String> questionList = new ArrayList<>(initialSize);
    	for(Node node: nodeSet.getNodeSortedList())
    	{
    		if(nodeSet.getDependencyMatrix().getOutDependencyList(node.getNodeId()).isEmpty())
    		{
    			questionList.add(node.getNodeName());
    		}
    	}
    	
    	String[] keywordArray = keyword.split("\\W+"); // split the keyword by none word character including whitespace.
    	int keywordArrayLength = keywordArray.length;
    	int numberOfMatched = 0;
    	for(String ruleName: questionList)
    	{
    		numberOfMatched = 0;
    		for(int i = 0; i < keywordArrayLength; i++)
    		{
    			if(ruleName.contains(keywordArray[i]))
    			{
    				numberOfMatched++;
    			}
    		}
    		if(numberOfMatched == keywordArrayLength)
    		{
    			conditionList.add(ruleName);
    		}
    	}
    	
    	return conditionList;
    }
}
