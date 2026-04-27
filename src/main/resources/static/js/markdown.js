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
			const url = this.file && this.file.startsWith('/') ? this.file : `/md/${this.file}`;
			const response=await fetch(url);

			if(!response.ok) throw new Error("get file error");

			const markdownText=await response.text();

			marked.use({
				breaks: true,
				gfm: true
			});

			const container=document.querySelector("[data-tab-content='docs']>.card");
			if(container){
				container.innerHTML=`<div class="md-body">${marked.parse(markdownText)}</div>`;
			}

			if(window.Prism){
				Prism.highlightAll();
			}
		} catch(err){
			console.log(err);
		}
	}
}