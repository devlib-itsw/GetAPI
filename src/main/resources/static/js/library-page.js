document.addEventListener("DOMContentLoaded", function () {
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
                            ${item.price}P / 호출
                        </span>
                    </div>

                    <p class="text-sm leading-relaxed text-muted line-clamp-2 mb-4">
                        ${item.description}
                    </p>

                    <div class="flex flex-wrap gap-1 mb-4">
                        ${(item.tags || []).map(tag => `
                            <span class="badge badge-secondary text-xs">${tag}</span>
                        `).join("")}
                    </div>

                    <div class="flex items-center justify-between border-t pt-3 text-sm text-muted">
                        <span>${item.authorName}</span>
                        <div class="flex items-center gap-3">
                            <span>${item.viewCount}</span>
                            <span>${item.starCount}</span>
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