document.addEventListener("DOMContentLoaded", function () {
	function priceIcon() {
	        return `
	            <svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
	                <circle cx="8" cy="8" r="6" />
	                <path d="M18.09 10.37A6 6 0 1 1 10.34 18" />
	                <path d="M7 6h1v4" />
	                <path d="m16.71 13.88.7.71-2.82 2.82" />
	            </svg>
	        `;
	    }

	    function eyeIcon() {
	        return `
	            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
	                <path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z" />
	                <circle cx="12" cy="12" r="3" />
	            </svg>
	        `;
	    }

	    function starIcon() {
	        return `
	            <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
	                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
	            </svg>
	        `;
	    }
	
	
    const keywordInput = document.querySelector('.input[type="text"]');
    const searchBtn = document.querySelector('.btn.btn-primary');

    const libraryPaginator = new DataPaginator({
        apiUrl: '/api/library/list',
        bodyContainer: '#library-grid',
        navContainer: '#library-nav',

        getSearchParams: () => ({
            keyword: keywordInput ? keywordInput.value.trim() : '',
            filters: [...document.querySelectorAll('[name="library-search"]:checked')]
                .map(el => el.value),
            sort: document.querySelector('.custom-select-item.selected')?.dataset.value ?? 'latest'
        }),

		renderRow: (item) => `
		    <a href="/user/library-view/${item.id}" class="card-interactive">
		        <div class="p-5">
		            <div class="flex items-start justify-between mb-3">
		                <h3 class="text-lg font-semibold text-foreground">${item.title}</h3>
		                <span class="price-tag" title="${item.price}P / 호출">
		                    ${priceIcon()}
		                    ${item.price}P / 호출
		                </span>
		            </div>

		            <p class="text-sm leading-relaxed text-muted line-clamp-2 mb-4">${item.description}</p>

		            <div class="flex flex-wrap gap-1 mb-4">
		                ${(item.tags || []).map(tag => `<span class="badge badge-secondary text-xs">${tag}</span>`).join("")}
		            </div>

		            <div class="flex items-center justify-between border-t pt-3 text-sm text-muted">
		                <span>${item.authorName}</span>
		                <div class="flex items-center gap-3">
		                    <span class="flex items-center gap-1">
		                        ${eyeIcon()}
		                        ${Number(item.viewCount).toLocaleString()}
		                    </span>
		                    <span class="flex items-center gap-1">
		                        ${starIcon()}
		                        ${Number(item.starCount).toLocaleString()}
		                    </span>
		                </div>
		            </div>
		        </div>
		    </a>
		`
    });

    libraryPaginator.load(0);

    if (searchBtn) {
        searchBtn.addEventListener('click', function () {
            libraryPaginator.load(0);
        });
    }

    if (keywordInput) {
        keywordInput.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                libraryPaginator.load(0);
            }
        });
    }
});