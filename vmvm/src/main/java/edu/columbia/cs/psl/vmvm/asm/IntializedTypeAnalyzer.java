package edu.columbia.cs.psl.vmvm.asm;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.columbia.cs.psl.vmvm.org.objectweb.asm.Opcodes;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.AbstractInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.FieldInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.MethodInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.TypeInsnNode;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.Analyzer;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.AnalyzerException;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.BasicInterpreter;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.Frame;
import edu.columbia.cs.psl.vmvm.org.objectweb.asm.tree.analysis.Interpreter;

public class IntializedTypeAnalyzer extends Analyzer {

	public HashSet<String> getTypesDefinitelyInitedStartingWith(Frame f) {
		return getTypesDefinitelyInitedStartingWith((Node) f);
	}


	private HashSet<String> getTypesDefinitelyInitedStartingWith(Node start) {
		if(start == null)
			throw new IllegalArgumentException("Starting frame must not be null");
		HashSet<Node> visited = new HashSet<>();
		LinkedList<Node> toVisit = new LinkedList<>();
		toVisit.add(start);
		Node n = null;
		while(!toVisit.isEmpty())
		{
			n = toVisit.pop();
			if(visited.contains(n))
				continue;
			visited.add(n);
//			toVisit.addAll(n.successors);
			switch(n.predecessors.size())
			{
			case 0:
				break;
			case 1:
				n.typesInstantiatedUpstream.addAll(n.predecessors.iterator().next().typesInstantiatedUpstream);
				break;
			default:
				HashSet<String>[] eachBranch = new HashSet[n.predecessors.size()];
				int i = 0;
				for (Node predecessor : n.predecessors) {
						eachBranch[i] = predecessor.typesInstantiatedUpstream;
					i++;
				}
				HashSet<String> tmp = eachBranch[0];
				for (i = 1; i < eachBranch.length; i++) {
					tmp.retainAll(eachBranch[i]);
				}
				n.typesInstantiatedUpstream.addAll(tmp);

				break;
			}
			if(n.typeInstantiated != null && n.typesInstantiatedUpstream.contains(n.typeInstantiated))
				n.isUnnecessary = true;
			if(n.typeInstantiated != null)
			n.typesInstantiatedUpstream.add(n.typeInstantiated);
			toVisit.addAll(n.successors);
		}
		return n.typesInstantiatedUpstream;
	}



	public IntializedTypeAnalyzer() {
		super(new TypeAnalyzerInterpreter());
	}

	@Override
	protected Frame newFrame(int nLocals, int nStack) {
		return new Node(nLocals, nStack);
	}

	@Override
	protected Frame newFrame(Frame src) {
		return new Node(src);
	}

	@Override
	protected void newControlFlowEdge(int insn, int successor) {
		Node from = (Node) getFrames()[insn];
		Node to = ((Node) getFrames()[successor]);
		from.successors.add(to);
		to.predecessors.add(from);
	}

	public static class Node extends Frame {
		Set<Node> successors;
		Set<Node> predecessors;
		String typeInstantiated;
		HashSet<String> typesInstantiatedUpstream = new HashSet<>();
		public boolean isUnnecessary;
		public Node(int nLocals, int nStack) {
			super(nLocals, nStack);
			successors = new HashSet<>();
			predecessors = new HashSet<>();
			typeInstantiated = null;

		}

		public Node(Frame src) {
			super(src);
			Node n = (Node) src;

			this.typeInstantiated = n.typeInstantiated;

		}

		public Frame init(Frame src) {
			super.init(src);
			successors = new HashSet<>();
			predecessors = new HashSet<>();
			typeInstantiated = null;
			return this;
		};

		@Override
		public void execute(AbstractInsnNode insn, Interpreter interpreter) throws AnalyzerException {
			switch (insn.getType()) {
			case AbstractInsnNode.FIELD_INSN:
				FieldInsnNode fin = (FieldInsnNode) insn;
				if (fin.getOpcode() == Opcodes.GETSTATIC || fin.getOpcode() == Opcodes.PUTSTATIC)
					typeInstantiated = fin.owner;
				break;
			case AbstractInsnNode.TYPE_INSN:
				TypeInsnNode tin = (TypeInsnNode) insn;
				if (tin.getOpcode() == Opcodes.NEW) {
					typeInstantiated = tin.desc;
				}
				break;
			case AbstractInsnNode.METHOD_INSN:
				MethodInsnNode min = (MethodInsnNode) insn;
				if (min.getOpcode() == Opcodes.INVOKESTATIC)
					typeInstantiated = min.owner;
				break;
			}
			super.execute(insn, interpreter);
		}

		@Override
		public String toString() {
			return "Frame [typesInstantiated=" + typeInstantiated + ", typesInstantiatedUpstream="+typesInstantiatedUpstream+", successors=" + successors.size() + "]";
		}
	}

	static class TypeAnalyzerInterpreter extends BasicInterpreter{
		
	}
}
