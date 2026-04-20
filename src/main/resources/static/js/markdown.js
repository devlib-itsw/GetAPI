/*const tempPath="/md/README.md";

const loadMarkdown=async ()=>{
	try{
		const response=await fetch(tempPath);
		
		if(!response.ok) throw new Error("get file error");
		
		const markdownText=await response.text();
		
		marked.setOptions({
			breaks: true,
			gfm: true
		});
		
		document.querySelector("#mdtest").innerHTML=marked.parse(markdownText);
		Prism.highlightAll();
	} catch(err){
		console.log(err);
	}
}

loadMarkdown();*/

class Markdown{
	file=null;
	
	constructor(path){
		this.file=path;
		
		this.load();
	}
	
	async load(){
		try{
			const response=await fetch(`/md/${this.file}`);
					
			if(!response.ok) throw new Error("get file error");
			
			const markdownText=await response.text();
			
			marked.setOptions({
				breaks: true,
				gfm: true
			});
			
			const container=document.querySelector("[data-tab-content='docs']>.card");
			if(container){
				container.innerHTML=marked.parse(markdownText);
			}

			if(window.Prism){
				Prism.highlightAll();
			}
		} catch(err){
			console.log(err);
		}
	}
}