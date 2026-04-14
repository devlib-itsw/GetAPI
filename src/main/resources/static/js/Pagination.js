class DataPaginator {
    constructor({ apiUrl, bodyContainer, navContainer, renderRow, getSearchParams }) {
        this.apiUrl = apiUrl;
        this.bodyContainer = document.querySelector(bodyContainer);
        this.navContainer = document.querySelector(navContainer);
        this.renderRow = renderRow; // 행을 그리는 함수
        this.getSearchParams = getSearchParams || (() => ({})); // 검색 조건을 가져오는 함수
        
        this.page = 0;
        this.data = null;

        // 버튼 클릭 이벤트 리스너 등록 (한 번만 실행)
        this.navContainer.addEventListener("click", e => this.handlePaginationClick(e));
    }

    // 서버에 데이터 요청 (보여주신 POST 방식 유지)
    async load(targetPage = 0) {
        this.page = targetPage;
        
        // 검색 조건과 페이지 번호를 합침
        const requestData = {
            page: this.page,
            ...this.getSearchParams() // keyword 등 다른 필드들을 여기서 합침
        };

        try {
            const response = await fetch(this.apiUrl, {
                method: "POST",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(requestData)
            });
            
            this.data = await response.json();
            this.render(); // 데이터 수신 후 화면 갱신
        } catch (error) {
            console.error("데이터 로딩 중 오류:", error);
        }
    }

    render() {
        if (!this.data) return;
        
        // 1. 리스트 출력
        this.bodyContainer.innerHTML = this.data.content.map(item => this.renderRow(item)).join("");
        
        // 2. 페이지네이션 출력
        this.renderPagination();
    }

    renderPagination() {
        const { totalPages, number } = this.data;
        if (!totalPages) {
            this.navContainer.innerHTML = "";
            return;
        }

        this.navContainer.innerHTML = [
            `<button class="btn btn-outline btn-icon btn-sm" data-control="prev">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="m15 18-6-6 6-6" />
                </svg>
            </button>`,
            Array.from({length: totalPages}, (_, i) => i).map(v => `
                <button data-page="${v}" class="btn btn-icon btn-sm ${v === number ? 'btn-primary' : 'btn-outline'}">${v + 1}</button>
            `).join(""),
            `<button class="btn btn-outline btn-icon btn-sm" data-control="next">
                <svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="m9 18 6-6-6-6" />
                </svg>
            </button>`
        ].join("");
    }

    handlePaginationClick(e) {// 4월 13일
        const btn = e.target.closest("button");
        if (!btn) return;

        const control = btn.dataset.control;
        const targetPage = btn.dataset.page;
        const maxPage = this.data ? this.data.totalPages - 1 : 0;

        let nextPage = this.page;
        if (control === "prev") nextPage = Math.max(this.page - 1, 0);
        else if (control === "next") nextPage = Math.min(this.page + 1, maxPage);
        else if (targetPage !== undefined) nextPage = Number(targetPage);

        if (this.page !== nextPage) {
            this.load(nextPage);
        }
    }
}