package rename.visitor;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import rename.common.Config;
import rename.model.DataModel;
import rename.model.ResultModel;
import rename.similarity.Recommend;
import rename.similarity.Similarity;
import rename.util.Util;



public class LocalVariableRefactorVisitor extends ASTVisitor {

	public String projectName = null;
	public List<ResultModel> results = null;
	public ResultModel result = null;
	public double maxSimilarity = 0.0;
	public DataModel renameData = null;
	
	public String visitingTypeName = null;
	public boolean isItSelf = false;
	public boolean isSuperOrSub = false;
	public ITypeBinding typeBinding = null;
	public ITypeBinding[] superInterfaces = null;
	public ITypeBinding superClass = null;
	public String packageName = "";
	public String typeName = "";
	public String methodName = "";
	
	public LocalVariableRefactorVisitor() {
		super();
		result = new ResultModel();
	}
	
	@Override
	public boolean visit(PackageDeclaration node) {
		this.packageName = node.getName().toString();
		return true;
	}

	@Override
	public boolean visit(AnonymousClassDeclaration node) {
		return false;
	}

	@Override
	public boolean visit(TypeDeclaration node) {
		if(visitingTypeName != null) return false;
		this.typeName = node.getName().toString();					
		ITypeBinding typeBinding = node.resolveBinding();
		if (typeBinding != null) {			
			visitingTypeName = node.getName().toString();				
			return true;
		}
		return false;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		if (visitingTypeName != null
				&& visitingTypeName.equals(node.getName().toString())) {
			visitingTypeName = null;
		}
	}		


	@Override
	public boolean visit(MethodDeclaration node) {
		IMethodBinding iMethod = node.resolveBinding();
		if(iMethod == null || iMethod.getJavaElement() == null){
			return false;
		}
		this.methodName = iMethod.getJavaElement().toString().substring(0, iMethod.getJavaElement().toString().indexOf("{")-1);
		String candidateName = node.getName().toString();
		if(!this.methodName.equals(renameData.methodName)) return false;
		//System.out.println("-------------method: " + candidateName);
	
		double similarity = Similarity.calculateSim(renameData, candidateName);		
		if(similarity > maxSimilarity ||
				(similarity == maxSimilarity && maxSimilarity > 0.0 && Similarity.isMoreSim(candidateName, result, renameData))){
			
			if(!Util.isOverriding(iMethod, renameData)){
				maxSimilarity = similarity;
				if(maxSimilarity >= Config.MIN_RECOMMEND_SIMILARITY){
					String suggestion = Recommend.detect(renameData, candidateName);
					

					SimpleName simpleName = node.getName();
					result.recommendStartPosition = simpleName.getStartPosition();
					result.recommendPackageName = this.packageName;
					result.recommendTypeName = this.typeName;
					result.recommendOriginalName = candidateName;
					result.recommendSubsequentName = suggestion;
					result.recommendRefactorType = "method";
					result.sim = maxSimilarity;
				}
			}
						
		}
		return true;

	}
	
	
	
	@Override
	public boolean visit(SingleVariableDeclaration node) {
		String candidateName = node.getName().toString();
		//System.out.println("-------------parameter: " + candidateName);
		double similarity = Similarity.calculateSim(renameData, candidateName);		
		if(similarity > maxSimilarity ||
				(similarity == maxSimilarity && maxSimilarity > 0.0 && Similarity.isMoreSim(candidateName, result, renameData))){
			maxSimilarity = similarity;
			if(maxSimilarity >= Config.MIN_RECOMMEND_SIMILARITY){
				String suggestion = Recommend.detect(renameData, candidateName);
				
				SimpleName simpleName = node.getName();
				result.recommendStartPosition = simpleName.getStartPosition();
				result.recommendPackageName = this.packageName;
				result.recommendTypeName = this.typeName;
				result.recommendOriginalName = candidateName;
				result.recommendSubsequentName = suggestion;
				result.recommendRefactorType = "localVariable";
				result.sim = maxSimilarity;
			}			
		}
		return true;
	}



	@Override
	public boolean visit(VariableDeclarationStatement node) {
		Object obj = node.fragments().get(0);
		if(obj instanceof VariableDeclarationFragment){
			String candidateName = ((VariableDeclarationFragment) obj).getName().toString();
			//System.out.println("-------------variable: " + candidateName);

			double similarity = Similarity.calculateSim(renameData, candidateName);		
			if(similarity > maxSimilarity ||
					(similarity == maxSimilarity && maxSimilarity > 0.0 && Similarity.isMoreSim(candidateName, result, renameData))){
				maxSimilarity = similarity;
				if(maxSimilarity >= Config.MIN_RECOMMEND_SIMILARITY){
					String suggestion = Recommend.detect(renameData, candidateName);
					
					SimpleName simpleName = ((VariableDeclarationFragment) obj).getName();
					result.recommendStartPosition = simpleName.getStartPosition();
					result.recommendPackageName = this.packageName;
					result.recommendTypeName = this.typeName;
					result.recommendOriginalName = candidateName;
					result.recommendSubsequentName = suggestion;
					result.recommendRefactorType = "localVariable";
					result.sim = maxSimilarity;
				}			
			}
		}
		
		return true;
	}
	
	
	@Override
	public boolean visit(MethodInvocation node) {
		String candidateName = node.getName().toString();
		//System.out.println("-------------methodInvocation: " + candidateName);
		IMethodBinding iMethod = node.resolveMethodBinding();
		if(iMethod == null) return true;
		if(!iMethod.getDeclaringClass().isFromSource()) return true;		
		String packageName = iMethod.getDeclaringClass().getPackage().getName();
		String typeName = iMethod.getDeclaringClass().getName();
		String methodName = iMethod.getJavaElement().toString().substring(0, iMethod.getJavaElement().toString().indexOf("{")-1);
		
			
		double similarity = Similarity.calculateSim(renameData, candidateName);		
		if(similarity > maxSimilarity ||
				(similarity == maxSimilarity && maxSimilarity > 0.0 && Similarity.isMoreSim(candidateName, result, renameData))){
			
			if(!Util.isOverriding(iMethod, renameData)){
				maxSimilarity = similarity;
				if(maxSimilarity >= Config.MIN_RECOMMEND_SIMILARITY){
					String suggestion = Recommend.detect(renameData, candidateName);
					
					result.recommendStartPosition = ((MethodDeclaration)iMethod.getMethodDeclaration()).getStartPosition();
					result.recommendPackageName = packageName;
					result.recommendTypeName = typeName;
					result.recommendOriginalName = candidateName;
					result.recommendSubsequentName = suggestion;
					result.recommendRefactorType = "method";	
					result.sim = maxSimilarity;
				}			
			}
			
		}
		return true;
	}
	
	@Override
	public boolean visit(FieldAccess node) {
		 if (!(node.getExpression() instanceof ThisExpression)) {
			 String candidateName = node.getName().toString();
			// System.out.println("-------------fieldAccess: " + candidateName);
			 ITypeBinding iType = node.resolveTypeBinding();
			 if(!iType.isFromSource()) return true;
			 String packageName = iType.getPackage().getName();
			 String typeName = iType.getName();
			 String methodName = "";
			 
			 			
			double similarity = Similarity.calculateSim(renameData, candidateName);		
			if(similarity > maxSimilarity ||
					(similarity == maxSimilarity && maxSimilarity > 0.0 && Similarity.isMoreSim(candidateName, result, renameData))){
				maxSimilarity = similarity;
				if(maxSimilarity >= Config.MIN_RECOMMEND_SIMILARITY){
					String suggestion = Recommend.detect(renameData, candidateName);
					

					SimpleName simpleName = node.getName();
					result.recommendStartPosition = simpleName.getStartPosition();
					result.recommendPackageName = packageName;
					result.recommendTypeName = typeName;
					result.recommendOriginalName = candidateName;
					result.recommendSubsequentName = suggestion;
					result.recommendRefactorType = "field";
					result.sim = maxSimilarity;
				}			
			}			
		}
		return true;
	}
	
	
}
