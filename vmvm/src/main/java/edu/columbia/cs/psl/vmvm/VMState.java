package edu.columbia.cs.psl.vmvm;

public class VMState {

		int vmID;
		int originalVMID;
		public VMState(int state, int originalState) {
			this.vmID = state;
			this.originalVMID = originalState;
		}
		void setState(int state) {
			this.vmID = state;
		}
		public void deVM()
		{
			vmID = originalVMID;
		}
		public int getState() {
			return vmID;
		}
}
